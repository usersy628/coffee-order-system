import http from 'k6/http';
import { check } from 'k6';

const baseUrl = __ENV.BASE_URL;

if (!baseUrl) {
  throw new Error('BASE_URL is required, for example http://host.docker.internal:18081');
}

const popularMenuUrl = `${baseUrl.replace(/\/$/, '')}/api/menus/popular`;

export const options = {
  scenarios: {
    popular_menu_baseline: {
      executor: 'constant-arrival-rate',
      rate: 30,
      timeUnit: '1s',
      duration: '5m',
      preAllocatedVUs: 10,
      maxVUs: 60,
      gracefulStop: '30s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
    dropped_iterations: ['count==0'],
    checks: ['rate==1'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

export default function () {
  const response = http.get(popularMenuUrl, {
    tags: { endpoint: 'popular-menu' },
  });

  check(response, {
    'returns HTTP 200': (result) => result.status === 200,
    'returns an items array': (result) => {
      try {
        return Array.isArray(result.json('items'));
      } catch (_) {
        return false;
      }
    },
  });
}
