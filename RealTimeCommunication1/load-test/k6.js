import {check, sleep} from 'k6';
import http from 'k6/http';
import ws from 'k6/ws';

let longPollingConnection = null;
let sseConnection = null;
let websocketConnection = null;
let stompClient = null;

export let options = {
    stages: [
        {duration: '20s', target: 300},
        {duration: '30s', target: 10000},
        {duration: '10s', target: 0},
    ],
};

const baseUrl = 'http://localhost:8080';
const wsUrl = 'ws://localhost:8080/ws';
const stompUrl = 'ws://localhost:8080/location';
const randomInt = getRandomInt();

function getRandomInt() {
    return Math.floor(Math.random() * 10) + 1;
}

function shortPollingTest() {
    http.get(`http://localhost:8080/cur/${randomInt}`);
}

function initializeLongPollingConnection() {
    if (longPollingConnection) {
        return longPollingConnection;
    }

    const connection = http.get(`${baseUrl}/location/long/${randomInt}`, {
        headers: {Accept: 'text/event-stream'},
        tags: {type: 'longPolling'}
    });
    check(connection, {'Long Polling Connection Status is 200': (r) => r.status === 200});
    return connection;
}

function initializeSSEConnection() {
    if (sseConnection) {
        return sseConnection;
    }
    const connection = http.get(`${baseUrl}/location/sse/connect`, {
        headers: {Accept: 'text/event-stream'},
        tags: {type: 'sse'}
    });
    check(connection, {'SSE Connection Status is 200': (r) => r.status === 200});
    return connection;
}

function initializeWebSocketConnection() {
    if (websocketConnection) {
        return websocketConnection;
    }

    const connection = ws.connect(wsUrl, function (socket) {
        socket.on('open', function () {
            socket.send('Hello from k6 WebSocket!');
        });

        socket.on('message', function (message) {
        });

        socket.on('close', function () {
        });
    });

    websocketConnection = connection;
    return connection;
}

function initializeStompClient(callback) {
    if (stompClient) {
        callback(stompClient); // 이미 stompClient가 존재하면 콜백 호출
        return;
    }

    ws.connect(stompUrl, {}, function (socket) {
        socket.on('open', function () {
            console.log('WebSocket connection opened');

            // STOMP CONNECT 메시지를 전송합니다.
            socket.send('CONNECT\naccept-version:1.0,1.1,2.0\n\n\x00\n');

            // STOMP SUBSCRIBE 메시지를 전송합니다.
            const subscribeMessage = `SUBSCRIBE\nid:${uuid()}\ndestination:/sub/location/${randomInt}\n\n\x00\n`;
            socket.send(subscribeMessage);

            // WebSocket 연결이 열렸을 때의 콜백 함수 호출
            callback(socket);
        });

        socket.on('message', function (message) {
        });

        socket.on('close', function () {
        });

        stompClient = socket; // WebSocket 객체를 stompClient에 할당
    });
}

function uuid() {
    const pattern = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx';
    return pattern.replace(/[xy]/g, function (c) {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}


export default function () {
    //shortPollingTest();
    // longPollingTest();
    // sseTest();
    // websocketTest();
    stompTest();

    sleep(3);
}

export function longPollingTest() {
    initializeLongPollingConnection();
    const url = `${baseUrl}/location/long/${randomInt}`;
    const response = http.get(url);
    check(response, {'Long Polling Post Status is 200': (r) => r.status === 200});

    const notifyResponse = http.post(`${baseUrl}/location/long/${randomInt}/notify`);
    check(notifyResponse, {'Long Polling Notify Status is 200': (r) => r.status === 200});
}

export function sseTest() {
    initializeSSEConnection();
    const postResponse = http.post(`${baseUrl}/location/sse/share`);
    check(postResponse, {'SSE Share Status is 200': (r) => r.status === 200});
}

export function websocketTest() {
    initializeWebSocketConnection();
    ws.connect(wsUrl, function (socket) {
        socket.on('open', function () {
            socket.send('Hello from k6!');
        });
    });
}

export function stompTest() {

    initializeStompClient(function (socket) {
        const stompMessage = `SEND\ndestination:/pub/share/${randomInt}\n\n${JSON.stringify({content: 'Hi from k6 STOMP!'})}\x00\n`;

        // WebSocket 연결이 열리면 STOMP 메시지를 전송합니다.
        socket.send(stompMessage);
    });
}
