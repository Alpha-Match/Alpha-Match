# CORS(Cross-Origin Resource Sharing) 설정 가이드

**문서 목적**: 프론트엔드(`http://localhost:3000`)에서 API 서버(`http://localhost:8080`)로 보내는 요청이 브라우저의 동일 출처 정책(Same-Origin Policy)에 의해 차단되는 문제를 해결하기 위함입니다.

**에러 메시지**: `[Network error]: TypeError: Failed to fetch`

---

## 1. 문제 원인

브라우저는 보안상의 이유로 스크립트에서 시작하는 교차 출처(cross-origin) HTTP 요청을 제한합니다. 현재 프론트엔드 개발 서버의 출처(origin)는 `http://localhost:3000`이고, API 서버의 출처는 `http://localhost:8080`으로 서로 다릅니다.

API 서버가 응답 헤더에 `Access-Control-Allow-Origin: http://localhost:3000` 와 같은 CORS 관련 헤더를 포함하여 명시적으로 허용하지 않으면, 브라우저는 API 요청을 차단합니다.

## 2. 해결 방안 (Spring WebFlux)

API 서버(`Api-Server`)는 Spring WebFlux를 사용하고 있으므로, 다음과 같이 WebFlux에 맞는 CORS 설정을 추가해야 합니다.

### `WebConfig` 또는 `WebFluxConfig` 클래스 생성 및 설정

`Api-Server` 프로젝트의 `config` 패키지에 다음 Java 클래스를 추가하거나 기존 설정 클래스에 내용을 병합해주세요.

```java
package com.alpha.api.config; // 패키지 경로는 실제 프로젝트 구조에 맞게 조정하세요.

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
@EnableWebFlux
public class WebConfig implements WebFluxConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로에 대해 CORS 설정을 적용
            .allowedOrigins("http://localhost:3000") // 프론트엔드 개발 서버의 출처를 허용
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 허용할 HTTP 메소드
            .allowedHeaders("*") // 모든 헤더를 허용
            .allowCredentials(true) // 쿠키 등 자격 증명을 허용
            .maxAge(3600); // pre-flight 요청의 캐시 시간 (초)
    }
}
```

### 주요 설정 설명

*   `addMapping("/**")`: API 서버의 모든 엔드포인트 (`/graphql` 포함)에 대해 설정을 적용합니다.
*   `allowedOrigins("http://localhost:3000")`: **가장 중요한 부분**으로, 프론트엔드 개발 서버의 주소를 명시적으로 허용합니다. 향후 프로덕션 배포 시에는 실제 프로덕션 도메인을 추가해야 합니다. (예: `.allowedOrigins("http://localhost:3000", "https://alpha-match.com")`)
*   `allowedMethods(...)`: GraphQL은 주로 `POST`를 사용하지만, 일반적인 REST API도 고려하여 주요 메소드를 모두 허용하는 것이 좋습니다.
*   `allowCredentials(true)`: 향후 인증 기능이 추가될 경우를 대비해 설정합니다.

## 3. 적용 방법

1.  위 `WebConfig.java` 파일을 `Api-Server`의 `config` 패키지에 추가합니다.
2.  `Api-Server`를 재시작합니다.

재시작 후 프론트엔드에서 API 요청 시 더 이상 `Failed to fetch` CORS 에러가 발생하지 않아야 합니다.
