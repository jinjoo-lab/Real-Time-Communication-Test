### Velog로 보기
[실시간 통신 분석 프로젝트 - 이론](https://velog.io/@jinjoo-lab/%EC%8B%A4%EC%8B%9C%EA%B0%84-%ED%86%B5%EC%8B%A0-%EB%B6%84%EC%84%9D-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8-%EC%9D%B4%EB%A1%A0)
[실시간 통신 분석 프로젝트 - Spring 구현 코드](https://velog.io/@jinjoo-lab/%EC%8B%A4%EC%8B%9C%EA%B0%84-%ED%86%B5%EC%8B%A0-%EB%B6%84%EC%84%9D-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8-%EA%B0%81-%EB%B0%A9%EB%B2%95%EC%9D%98-%EA%B5%AC%ED%98%84-%EC%BD%94%EB%93%9C)
[실시간 통신 분석 프로젝트 - 테스트 분석](https://velog.io/@jinjoo-lab/%EC%8B%A4%EC%8B%9C%EA%B0%84-%ED%86%B5%EC%8B%A0-%EB%B6%84%EC%84%9D-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8-%ED%85%8C%EC%8A%A4%ED%8A%B8-%EC%A7%80%ED%91%9C-%EB%B6%84%EC%84%9D)

![image](https://github.com/jinjoo-lab/Real-Time-Communication-Test/assets/84346055/4251618a-2eb9-491f-974c-66deba267104)

## 시나리오

> 테스트 시나리오를 구성할 때 몇가지 상황을 고려하였다.
>
- 서비스가 시작되고 **사용자들이 진입**하는 상황 : **정상 진입 확인**
- **사용자 최고치(최대 처리량)에서 서비스 요청 상황** : **최대 부하 확인**
- 사용자들의 요청이 완료되고 **사용자 수가 줄어드면서**의 상황 : **회복성 확인**

> 총 테스트 시간을 1분으로 설정했고 위 3가지 상황을 고려하여 3가지 구간으로 나눠서 시나리오를 구성했다.
>

### 테스트 시나리오

1. 서비스가 시작되고 나서 사용자 수가 유입되는 시간을 20초로 설정하였다.
2. 20초 후에는 많은 사용자가 유입되서 통신을 요청하도록 설정하였다.
    - **1000 , 5000 , 10000명**의 최대 사용자를 기준으로 테스트하였다.
3. 회복성을 보기 위해 나머지 10초 동안에는 유입되는 사용자를 0으로 설정하여 앞서 설정한 사용자들이 점진적으로 통신을 종료하게끔 작성했다.

![image](https://github.com/jinjoo-lab/Real-Time-Communication-Test/assets/84346055/d2ac1dd5-baf8-4c84-868b-be4c1754d49e)
# 분석

> 최고 접속자의 수를 1000, 5000, 10000명으로 설정하고 각 실시간 통신에 대한 시나리오 테스트를 수행했다. 테스트 결과를 3가지 측면에서 분석하였다.
>

## Polling VS Long Polling

---

> 우선 Polling과 Long Polling을 10000명의 사용자 지표에 따른 지표를 바탕으로 비교해보았다.
>

### iteration_duration / interations

> iteration_duration: 한 번의 반복(iteration)에 걸리는 시간을 측정한 것
interations : 테스트 시간 동안 반복된 횟수 (전체 횟수 , 초당 횟수)
>

```
Polling : 
	iteration_duration.........: p(90)=4.47s p(95)=4.94s
  iterations.................: 63081   770.071737/s

Long Polling : 
	iteration_duration.............: p(90)=17.07s p(95)=19.92s  
  iterations.....................: 19122  214.396293/s
```

> 위 지표를 보면 Polling이 **총 수행 횟수가 더 많고 한번의 주기 시간은 짧은 것**을 알 수 있다.
>
- 서버로부터 데이터가 오지 않더라도 API를 응답하는 Polling의 특성 상 호출 횟수가 더 많은 것이다.
- 반면 데이터가 오기 까지 연결을 지속하는 Long Polling은 평균 수행 시간이 더 긴 것을 알 수 있는데 Long Polling 자체에 설정한 TimeOut 값에 따라 결과는 달라질 것이다.

### Heap Used

- Polling

![image](https://github.com/jinjoo-lab/Real-Time-Communication-Test/assets/84346055/54bab203-23f9-4a3c-8728-25e24d1fbd71)

- Long Polling

![image](https://github.com/jinjoo-lab/Real-Time-Communication-Test/assets/84346055/10f48633-bfbe-41a2-be97-23b7f001c6e5)

> 다음은 Spring 자체의 Heap 사용량에 대한 비교이다. 지표를 분석해보면 Long Polling이 Polling 방식에 비해 Heap 사용량이 더 많은 것을 알 수 있다.
>
- 이에 대한 이유는 Long Polling 방식을 구현하는데 사용한 DeferredResult 때문이다.
- Polling 방식의 경우에는 결과값을 반환하고 더 이상 서버에서 유지하지 않는다. 하지만 Long Polling의 방식에는 DeferredResult 객체에 setResult() 메서드를 통한 값이 삽입되기 전까지 데이터가 유지되어야 하기 때문에 Heap 사용량이 더 많을 수밖에 없다.

## Polling VS WebSocket

---

> 두번째로 Polling 방식과 WebSocket 방식의 비교이다. 두 방식에 대해 k6에서 표로 제공하는 지표는 차이가 있다. 통신 방식에 차이가 있기 때문에 서로 다른 지표에 대한 비교는 어려웠다. 그래서 prometheus의 지표를 중심으로 비교했다.
>
- 두 방식에 대해 보통적으로 말하는 비교라면 WebSocket이 connection의 횟수가 적어 **자원 사용량이 적을 것**이라는 것이다 !!!!!!
- **자원 사용량**을 중심으로 비교해보겠다.

### System CPU Usage / Process CPU Usage

> System : 운영 체제 전체에서 모든 프로세스가 차지하는 CPU 사용량
Process : 특정 프로세스가 차지하는 CPU 사용량
>
- Polling

![image](https://github.com/jinjoo-lab/Real-Time-Communication-Test/assets/84346055/bd0fd9a6-0d5b-4730-9b65-a87c270243df)

- WebSocket

![image](https://github.com/jinjoo-lab/Real-Time-Communication-Test/assets/84346055/6a79d26a-05cd-4155-952f-33beb1353557)

> WebSocket이 평균적으로 Polling 방식과 비교하여 CPU 사용량이 적은 것을 알 수 있다.
>
- 매번 connection을 생성하고 종료하는 과정이 삭제되어 전체적인 CPU 사용량에도 영향을 준 것을 알 수 있다.

### Heap Use

- Polling

![image](https://github.com/jinjoo-lab/Real-Time-Communication-Test/assets/84346055/9c8c6358-823a-4280-8d05-7ac16751b76c)

- WebSocket

![image](https://github.com/jinjoo-lab/Real-Time-Communication-Test/assets/84346055/d1fba1a9-73b1-4b7b-9d35-00bad6ddfcf2)

> 하지만 Spring 자체의 Heap 사용량은 WebSocket이 더 높게 측정되는 것을 알 수 있다.
>
- 이는 WebSocketSession 객체 때문이다. WebSocket Session을 계속 유지하며 지속적으로 Heap에 존재하게 되므로 커넥션을 유지하지 않는 ShortPolling 보다 WebSocket이 Heap 사용량이 더 많다.

## WebSocket VS STOMP

---

> 마지막으로 WebSocket과 STOMP에 대한 비교이다. k6상에서 두 방식에 대해 동일한 지표를 제공한다. 각 지표로 확인할 수 있는 정보는 다음과 같다.
>
- **반복 작업**: `iteration_duration`과 `iterations`는 테스트 반복 주기와 성능
- **사용자 부하**: `vus`와 `vus_max`는 가상의 사용자 부하 (테스트 상에서는 동일)
- **WebSocket 성능**: `ws_connecting`과 `ws_session_duration`은 WebSocket 연결과 세션의 성능
- **메시지 처리**: `ws_msgs_received`와 `ws_msgs_sent`는 WebSocket을 통해 처리된 메시지 수

### ws_msgs_sent, ws_msgs_received, iterations

- WebSocket

    ```
    ws_msgs_received......: 1336749 14836.253751/s
    ws_msgs_sent..........: 8705    96.614689/s
    iterations............: 136     1.509431/s
    ```

- STOMP

    ```
    ws_msgs_received......: 3409118 37873.839154/s
    ws_msgs_sent..........: 24696   274.361
    iterations............: 878     9.754204/s
    ```


> STOMP가 WebSocket과 비교해서 3가지 지표에서 더 높다는 것을 알 수 있다.
>
- 이는 동일한 **테스트 시간동안 더 많은 메시지를 처리**한 것이고 STOMP가 WebSocket과 비교하여 처리량이 더 높다는 것을 알 수 있다.
- 이에 대해 메시지 브로커를 통한 처리가 큰 이유라고 생각한다. 그룹 별로 나눠지 세션에 메시지를 분배하고 통신하는 과정을 WebSocket을 사용할 때는 사용자가 직접 구현해줬고 이에 대한 성능이 최적화되어 있는 메시지 브로커보다는 효율이 떨어진다 판단했다.

### ws_sessions, ws_connecting

```
WebSocket :
	ws_connecting.........: avg=11.86s min=590.66µs med=3.78s  max=43.12s
	ws_sessions...........: 8771    97.347207/s
Stomp : 
	ws_connecting.........: avg=5.54s  min=434.79µs med=354.49ms max=43.78s
	ws_sessions...........: 9844    109.362619/s
```

> 
평균적으로 STOMP가 WebSocket에 비해 **연결 설정에 더 적은 시간을 소비**하는 것을 알 수 있다.
가장 주의 깊게 본 지표는 ws_sessions이다. 해당 지표는 테스트가 수행되는 동안 성공적으로 열린 websocket session 수이다.
>
- 해당 지표에서 10000의 사용자를 대상으로 STOMP가 더 많은 websocket session을 포함한 것을 알 수 있다. 즉 **부하에 더 강하다는 것**이다.
