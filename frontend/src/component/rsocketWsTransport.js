// rsocketWsTransport.ts
// ✅ 정적 import: default + 두 가지 이름 후보를 모두 시도
import DefaultCtor, {WebsocketClientTransport,} from 'rsocket-websocket-client';

/**
 * 현재 설치된 rsocket-websocket-client가 무엇을 export 하든,
 * 존재하는 생성자를 골라서 반환한다.
 */
export function createWebSocketTransport(options) {
    const Ctor =
        WebsocketClientTransport
        ?? DefaultCtor;

    if (typeof Ctor !== 'function') {
        // 현재 모듈 export가 예상과 다른 경우 → 콘솔에서 확인
        console.error('[RSocket] Could not resolve WebSocket transport constructor', {
            DefaultCtor,
            WebsocketClientTransport,
        });
        throw new TypeError('WebSocket transport constructor not found from rsocket-websocket-client');
    }
    return new Ctor(options);
}
