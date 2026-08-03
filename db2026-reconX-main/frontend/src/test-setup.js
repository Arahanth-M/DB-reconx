import '@testing-library/jest-dom/vitest';

class MockEventSource {
  constructor(url) {
    this.url = url;
    this.onopen = null;
    this.onmessage = null;
    this.onerror = null;
  }

  close() {}
}

global.EventSource = MockEventSource;