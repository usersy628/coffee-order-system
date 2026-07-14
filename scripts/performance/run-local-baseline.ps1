[CmdletBinding()]
param(
    [string]$ApplicationBaseUrl = 'http://localhost:18081',
    [string]$K6BaseUrl = 'http://host.docker.internal:18081',
    [string]$K6Image = 'grafana/k6:0.54.0',
    [switch]$Seed,
    [switch]$RunK6
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$ProjectName = 'coffee-order-system-perf'
$PerfDatabase = 'coffee_order_perf'
$RequiredEnvironmentVariables = @('PERF_MYSQL_USER', 'PERF_MYSQL_PASSWORD', 'PERF_MYSQL_ROOT_PASSWORD')
$ExpectedFixture = [ordered]@{
    orders = 100000
    orderItems = 300000
    menus = 100
}
$RepositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$ComposeFile = Join-Path $RepositoryRoot 'docker-compose.performance.yml'
$SeedScript = Join-Path $PSScriptRoot 'seed-popularity.sql'
$SnapshotScript = Join-Path $PSScriptRoot 'mysql-snapshot.sql'
$K6ScriptDirectory = $PSScriptRoot

function Assert-LastExitCode {
    param([string]$Operation)

    if ($LASTEXITCODE -ne 0) {
        throw "$Operation failed with exit code $LASTEXITCODE."
    }
}

function Get-DockerText {
    param([string[]]$Arguments)

    $output = & docker @Arguments
    Assert-LastExitCode "docker $($Arguments[0])"
    return ($output -join [Environment]::NewLine).Trim()
}

function Get-GitText {
    param([string[]]$Arguments)

    $output = & git @Arguments
    Assert-LastExitCode "git $($Arguments[0])"
    return ($output -join [Environment]::NewLine).Trim()
}

function Assert-RequiredEnvironment {
    foreach ($name in $RequiredEnvironmentVariables) {
        if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
            throw "$name must be set in this PowerShell process and in the IntelliJ or Gradle JVM that starts local,perf."
        }
    }
}

function Get-PerfMySqlPort {
    $value = if ([string]::IsNullOrWhiteSpace($env:PERF_MYSQL_PORT)) { '3308' } else { $env:PERF_MYSQL_PORT }
    [int]$port = 0
    if (-not [int]::TryParse($value, [ref]$port) -or $port -lt 1 -or $port -gt 65535) {
        throw 'PERF_MYSQL_PORT must be a TCP port between 1 and 65535.'
    }
    return $port
}

function Assert-LocalHttpUrl {
    param(
        [string]$Value,
        [string]$Name,
        [string[]]$AllowedHosts
    )

    $uri = [Uri]$Value
    if (-not $uri.IsAbsoluteUri -or $uri.Scheme -notin @('http', 'https') -or $uri.Host -notin $AllowedHosts) {
        throw "$Name must be an HTTP(S) URL for one of: $($AllowedHosts -join ', ')."
    }
    if (-not [string]::IsNullOrEmpty($uri.Query) -or -not [string]::IsNullOrEmpty($uri.Fragment)) {
        throw "$Name must not include a query string or fragment."
    }
    if ($uri.AbsolutePath -ne '/') {
        throw "$Name must not include a path."
    }
}

function Assert-K6TargetsPerformanceApplication {
    $applicationUri = [Uri]$ApplicationBaseUrl
    $k6Uri = [Uri]$K6BaseUrl
    if ($applicationUri.Scheme -ne $k6Uri.Scheme -or $applicationUri.Port -ne $k6Uri.Port) {
        throw 'K6BaseUrl must use the same scheme and port as ApplicationBaseUrl so the load generator cannot target a different local application.'
    }
}

function Wait-ForPerformanceApplication {
    $healthUrl = "$($ApplicationBaseUrl.TrimEnd('/'))/actuator/health"
    $infoUrl = "$($ApplicationBaseUrl.TrimEnd('/'))/actuator/info"

    for ($attempt = 1; $attempt -le 30; $attempt++) {
        try {
            $health = Invoke-RestMethod -Uri $healthUrl -TimeoutSec 5
        } catch {
            Start-Sleep -Seconds 2
            continue
        }

        if ($health.status -eq 'UP') {
            try {
                $info = Invoke-RestMethod -Uri $infoUrl -TimeoutSec 5
            } catch {
                throw "An application is already healthy at $ApplicationBaseUrl but it is not the local,perf profile. The runner will not create or seed a perf database for it."
            }
            if ($info.s11.'baseline-profile' -eq 'perf' -and
                $info.s11.database -eq $PerfDatabase -and
                $info.s11.host -eq '127.0.0.1' -and
                $info.s11.port -eq $PerfMySqlPort) {
                return
            }
            if ($info.s11.'baseline-profile' -eq 'perf' -and $info.s11.connection -eq 'not-verified') {
                Start-Sleep -Seconds 2
                continue
            }
            throw "An application is already healthy at $ApplicationBaseUrl but it did not prove the required local,perf datasource identity."
        }
        Start-Sleep -Seconds 2
    }

    throw "The application did not prove the local,perf profile through $infoUrl. Start it with SPRING_PROFILES_ACTIVE=local,perf and the same PERF_MYSQL_* values."
}

function Invoke-PerformanceMySqlScript {
    param(
        [string]$ScriptPath,
        [string]$OutputPath
    )

    $scriptContents = [IO.File]::ReadAllText($ScriptPath)
    $composeArguments = @(
        'compose', '-p', $ProjectName, '-f', $ComposeFile,
        'exec', '-T',
        'mysql-perf', 'sh', '-ec', 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql -u "$MYSQL_USER" coffee_order_perf'
    )
    $output = $scriptContents | & docker @composeArguments 2>&1
    $exitCode = $LASTEXITCODE
    $output | Set-Content -Path $OutputPath -Encoding utf8
    if ($exitCode -ne 0) {
        throw "MySQL script $([IO.Path]::GetFileName($ScriptPath)) failed with exit code $exitCode."
    }
}

function Assert-PerformanceFixture {
    $fixtureSql = @"
SELECT DATABASE(),
       (SELECT COUNT(*) FROM orders WHERE id >= 1000000 AND id < 1100000),
       (SELECT COUNT(*) FROM order_item WHERE id >= 2000000 AND id < 2300000),
       (SELECT COUNT(*) FROM menu WHERE id >= 10000 AND id < 10100);
"@
    $composeArguments = @(
        'compose', '-p', $ProjectName, '-f', $ComposeFile,
        'exec', '-T',
        'mysql-perf', 'sh', '-ec', 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql --batch --skip-column-names -u "$MYSQL_USER" coffee_order_perf'
    )
    $output = $fixtureSql | & docker @composeArguments 2>&1
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0) {
        throw "Fixture verification failed with exit code $exitCode."
    }

    $lines = @(
        $output |
        ForEach-Object { $_.ToString().Trim() } |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
    )
    if ($lines.Count -ne 1) {
        throw 'Fixture verification did not return exactly one data row.'
    }

    $values = $lines[0] -split "`t"
    if ($values.Count -ne 4) {
        throw 'Fixture verification returned an unexpected column count.'
    }

    try {
        $orders = [int]$values[1]
        $orderItems = [int]$values[2]
        $menus = [int]$values[3]
    } catch {
        throw 'Fixture verification returned non-numeric counts.'
    }

    if ($values[0] -ne $PerfDatabase -or
        $orders -ne $ExpectedFixture.orders -or
        $orderItems -ne $ExpectedFixture.orderItems -or
        $menus -ne $ExpectedFixture.menus) {
        throw "Fixture verification failed. Expected $PerfDatabase with orders=$($ExpectedFixture.orders), orderItems=$($ExpectedFixture.orderItems), menus=$($ExpectedFixture.menus); received $($values[0]) with orders=$orders, orderItems=$orderItems, menus=$menus."
    }

    return [ordered]@{
        database = $values[0]
        orders = $orders
        orderItems = $orderItems
        menus = $menus
        verifiedAtUtc = [DateTime]::UtcNow.ToString('o')
    }
}

function Get-HikariMetric {
    param([string]$MetricName)

    try {
        $metric = Invoke-RestMethod -Uri "$($ApplicationBaseUrl.TrimEnd('/'))/actuator/metrics/$MetricName" -TimeoutSec 5
        $value = $metric.measurements | Where-Object { $_.statistic -eq 'VALUE' } | Select-Object -First 1
        return [ordered]@{
            name = $MetricName
            value = if ($null -eq $value) { $null } else { $value.value }
        }
    } catch {
        return [ordered]@{
            name = $MetricName
            error = $_.Exception.Message
        }
    }
}

function Get-HikariSnapshot {
    return [ordered]@{
        capturedAtUtc = [DateTime]::UtcNow.ToString('o')
        metrics = @(
            Get-HikariMetric 'hikaricp.connections.active'
            Get-HikariMetric 'hikaricp.connections.pending'
            Get-HikariMetric 'hikaricp.connections.max'
        )
    }
}

function Start-HikariSampler {
    return Start-Job -ArgumentList $ApplicationBaseUrl -ScriptBlock {
        param([string]$BaseUrl)

        $metricNames = @(
            'hikaricp.connections.active',
            'hikaricp.connections.pending',
            'hikaricp.connections.max'
        )

        while ($true) {
            $metrics = @(
                foreach ($metricName in $metricNames) {
                    try {
                        $metric = Invoke-RestMethod -Uri "$($BaseUrl.TrimEnd('/'))/actuator/metrics/$metricName" -TimeoutSec 5
                        $value = $metric.measurements | Where-Object { $_.statistic -eq 'VALUE' } | Select-Object -First 1
                        [pscustomobject]@{
                            name = $metricName
                            value = if ($null -eq $value) { $null } else { $value.value }
                        }
                    } catch {
                        [pscustomobject]@{
                            name = $metricName
                            error = $_.Exception.Message
                        }
                    }
                }
            )
            [pscustomobject]@{
                capturedAtUtc = [DateTime]::UtcNow.ToString('o')
                metrics = $metrics
            }
            Start-Sleep -Seconds 5
        }
    }
}

function Stop-HikariSampler {
    param(
        [object]$Job,
        [string]$OutputPath
    )

    Stop-Job -Job $Job -ErrorAction SilentlyContinue
    $samples = @(Receive-Job -Job $Job -ErrorAction SilentlyContinue)
    Remove-Job -Job $Job -Force -ErrorAction SilentlyContinue
    $samples | ConvertTo-Json -Depth 8 | Set-Content -Path $OutputPath -Encoding utf8
}

function Assert-RunningPerformanceMySql {
    $containerId = Get-DockerText -Arguments @(
        'compose', '-p', $ProjectName, '-f', $ComposeFile,
        'ps', '-q', 'mysql-perf'
    )
    if ([string]::IsNullOrWhiteSpace($containerId)) {
        throw "The standalone mysql-perf service is not running. Start it first with: docker compose -p $ProjectName -f docker-compose.performance.yml up -d --wait mysql-perf"
    }

    $health = Get-DockerText -Arguments @(
        'inspect', '--format', '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}', $containerId
    )
    if ($health -ne 'healthy') {
        throw "The standalone mysql-perf service is not healthy. Start it first with: docker compose -p $ProjectName -f docker-compose.performance.yml up -d --wait mysql-perf"
    }
}

function Write-EnvironmentSnapshot {
    param(
        [string]$OutputPath,
        [object]$Fixture
    )

    $cpu = Get-CimInstance Win32_Processor | Select-Object -First 1
    $computerSystem = Get-CimInstance Win32_ComputerSystem
    $operatingSystem = Get-CimInstance Win32_OperatingSystem
    $javaVersion = try { (& java -version 2>&1) -join [Environment]::NewLine } catch { $_.Exception.Message }
    $snapshot = [ordered]@{
        capturedAtUtc = [DateTime]::UtcNow.ToString('o')
        git = [ordered]@{
            commit = Get-GitText -Arguments @('-C', $RepositoryRoot, 'rev-parse', 'HEAD')
            worktreeStatus = Get-GitText -Arguments @('-C', $RepositoryRoot, 'status', '--short')
        }
        host = [ordered]@{
            operatingSystem = $operatingSystem.Caption
            version = $operatingSystem.Version
            cpuModel = $cpu.Name.Trim()
            physicalCores = $cpu.NumberOfCores
            logicalCores = $cpu.NumberOfLogicalProcessors
            ramGiB = [Math]::Round($computerSystem.TotalPhysicalMemory / 1GB, 2)
        }
        runtime = [ordered]@{
            dockerEngine = Get-DockerText -Arguments @('version', '--format', '{{.Server.Version}}')
            dockerCpu = Get-DockerText -Arguments @('info', '--format', '{{.NCPU}}')
            dockerMemoryBytes = Get-DockerText -Arguments @('info', '--format', '{{.MemTotal}}')
            java = $javaVersion
            k6Image = $K6Image
        }
        topology = [ordered]@{
            applicationInstances = 1
            applicationBaseUrl = $ApplicationBaseUrl
            k6BaseUrl = $K6BaseUrl
            activeProfiles = 'local,perf'
            database = $PerfDatabase
            databaseHostPort = $PerfMySqlPort
        }
        dataset = [ordered]@{
            orders = $Fixture.orders
            orderItems = $Fixture.orderItems
            menus = $Fixture.menus
            period = '30 days'
            verifiedAtUtc = $Fixture.verifiedAtUtc
        }
        hikariBefore = Get-HikariSnapshot
    }

    $snapshot | ConvertTo-Json -Depth 8 | Set-Content -Path $OutputPath -Encoding utf8
}

Assert-RequiredEnvironment
Assert-LocalHttpUrl -Value $ApplicationBaseUrl -Name 'ApplicationBaseUrl' -AllowedHosts @('localhost', '127.0.0.1', 'host.docker.internal')
Assert-LocalHttpUrl -Value $K6BaseUrl -Name 'K6BaseUrl' -AllowedHosts @('host.docker.internal')
Assert-K6TargetsPerformanceApplication
if ($RunK6 -and -not $Seed) {
    throw '-RunK6 requires -Seed so the 100000/300000 fixture is rebuilt and verified immediately before measurement.'
}
$PerfMySqlPort = Get-PerfMySqlPort

Wait-ForPerformanceApplication
Assert-RunningPerformanceMySql

$resultDirectory = Join-Path $RepositoryRoot (Join-Path 'build\performance\s11' ([DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ')))
New-Item -ItemType Directory -Path $resultDirectory -Force | Out-Null

if ($Seed) {
    Invoke-PerformanceMySqlScript -ScriptPath $SeedScript -OutputPath (Join-Path $resultDirectory 'seed-output.txt')
}

if ($RunK6) {
    $fixture = Assert-PerformanceFixture
    Invoke-PerformanceMySqlScript -ScriptPath $SnapshotScript -OutputPath (Join-Path $resultDirectory 'mysql-before.txt')
    Write-EnvironmentSnapshot -OutputPath (Join-Path $resultDirectory 'environment.json') -Fixture $fixture

    $k6Arguments = @(
        'run', '--rm', '--add-host', 'host.docker.internal:host-gateway',
        '--mount', "type=bind,source=$K6ScriptDirectory,target=/scripts,readonly",
        '--mount', "type=bind,source=$resultDirectory,target=/results",
        $K6Image,
        'run', '-e', "BASE_URL=$K6BaseUrl", '--summary-export', '/results/k6-summary.json', '/scripts/popular-menu.js'
    )
    $k6ExitCode = $null
    $hikariSampler = Start-HikariSampler
    try {
        & docker @k6Arguments
        $k6ExitCode = $LASTEXITCODE
    } finally {
        Stop-HikariSampler -Job $hikariSampler -OutputPath (Join-Path $resultDirectory 'hikari-samples.json')
        Invoke-PerformanceMySqlScript -ScriptPath $SnapshotScript -OutputPath (Join-Path $resultDirectory 'mysql-after.txt')
        Get-HikariSnapshot | ConvertTo-Json -Depth 8 | Set-Content -Path (Join-Path $resultDirectory 'hikari-after.json') -Encoding utf8
        $k6Digest = Get-DockerText -Arguments @('image', 'inspect', $K6Image, '--format', '{{index .RepoDigests 0}}')
        Set-Content -Path (Join-Path $resultDirectory 'k6-image-digest.txt') -Value $k6Digest -Encoding utf8
    }
    if ($k6ExitCode -ne 0) {
        throw "k6 failed its run or thresholds with exit code $k6ExitCode. Review the artifacts in $resultDirectory."
    }
}

Write-Host "S11 artifacts: $resultDirectory"
