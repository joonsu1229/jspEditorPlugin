# JSP Lite 바이브코딩 가이드

이 문서는 JSP Lite IntelliJ 플러그인을 AI 코딩 도구와 함께 이어서 개발할 때 필요한 프로젝트 맥락을 정리한 파일입니다.

## 한 줄 요약

JSP Lite는 IntelliJ IDEA Community Edition에서 JSP 파일을 plain text가 아니라 JSP/HTML/EL/JavaScript/CSS가 섞인 파일처럼 읽고 편집하게 해주는 경량 에디터 플러그인입니다.

## 제품 목표

Community Edition 사용자가 레거시 JSP 프로젝트를 열었을 때 최소한 아래 작업을 편하게 할 수 있어야 합니다.

- `.jsp`, `.jspf`, `.jspx`, `.tag`, `.tagx` 파일 인식
- JSP/HTML/EL/script/style 문법 색상 구분
- include, forward, script 함수 위치로 이동
- JSP directive, JSTL, EL implicit object 자동완성
- 라이트/다크 테마 모두에서 읽을 수 있는 색상

목표는 "Ultimate Edition의 JSP 지원 복제"가 아닙니다. 레거시 JSP를 Community Edition에서 빠르게 읽고 수정하는 실용적인 보조 도구입니다.

## 현재 버전

- Plugin version: `0.1.6`
- 설치 ZIP: `build/distributions/jsp-lite-intellij-plugin-0.1.6.zip`
- 0.1.6 핵심 변경: 다크 테마 기준 단일 RGB 제거, `JBColor(light, dark)`와 IntelliJ 기본 Color Scheme fallback 사용

## 중요한 설계 원칙

1. 무거운 PSI를 만들지 않는다.
2. JSP runtime, servlet model, Java type resolver를 구현하지 않는다.
3. Community Edition에서 동작하는 에디터 편의 기능에 집중한다.
4. 정규식/문자열 기반 탐색은 작은 범위에서 실용적으로만 사용한다.
5. 색상은 단일 고정 RGB보다 `JBColor(light, dark)`, IntelliJ `TextAttributesKey`, fallback 색상 키를 우선한다.
6. 기능을 추가할 때는 레거시 JSP 파일에서 흔한 패턴을 우선한다.

## 프로젝트 구조

```text
src/main/java/com/github/codex/jsplite/
  JspLiteLanguage.java
  JspLiteFileType.java
  JspLiteParserDefinition.java
  JspLiteIcons.java

  lexer/
    JspLiteLexer.java
    JspLiteTokenType.java
    JspLiteTokenTypes.java

  highlighting/
    JspLiteSyntaxHighlighter.java
    JspLiteSyntaxHighlighterFactory.java
    JspLiteColorSettingsPage.java

  completion/
    JspLiteCompletionContributor.java

  editor/
    JspLiteCommenter.java

  navigation/
    JspLiteReferenceContributor.java
    JspLiteGotoDeclarationHandler.java
```

## 핵심 파일별 역할

### `JspLiteLexer.java`

JSP Lite의 가장 중요한 파일입니다. 별도 grammar generator 없이 `LexerBase`를 직접 구현합니다.

처리하는 주요 상태:

- top-level HTML/text
- HTML tag
- `<script>`
- `<style>`
- JSP directive: `<%@ ... %>`
- JSP scriptlet: `<% ... %>`
- JSP expression: `<%= ... %>`
- JSP declaration: `<%! ... %>`
- EL: `${...}`, `#{...}`

주의할 점:

- `<script>`와 `<style>` 내부에서도 JSP/EL이 나올 수 있습니다.
- HTML tag attribute 안에서도 JSP/EL이 나올 수 있습니다.
- lexer는 완전한 parser가 아니므로 지나치게 복잡한 문법 검증을 넣지 않는 편이 낫습니다.

### `JspLiteSyntaxHighlighter.java`

토큰 타입을 IntelliJ 색상 키에 매핑합니다.

현재 방향:

- 단일 테마 기준 `Color(...)` 사용 금지
- 플러그인 기본색이 필요한 경우 `JBColor(lightColor, darkColor)` 사용
- `createTextAttributesKey(name, attributes)`와 `setFallbackAttributeKey(fallbackKey)` 사용
- fallback은 `DefaultLanguageHighlighterColors`를 우선 사용

0.1.6에서 단일 고정 RGB 색상을 제거한 이유:

- 기존 색상이 다크 테마 기준이라 라이트 테마에서 가독성이 떨어졌습니다.
- `JBColor`를 쓰면 변수/함수/문자열 구분감을 유지하면서 라이트/다크 테마별 대비를 줄 수 있습니다.
- IntelliJ Color Scheme fallback을 함께 걸어 커스텀 테마에서도 기본 동작을 유지합니다.

### `JspLiteColorSettingsPage.java`

`Settings > Editor > Color Scheme > JSP Lite`에 노출되는 색상 설정 페이지입니다.

사용자가 조정할 수 있는 항목:

- JSP directive
- JSP scriptlet/declaration
- JSP expression
- JSP comment
- EL expression
- HTML tag/attribute/string/comment
- Code keyword/identifier/function/variable/parameter
- CSS selector/property/value
- Code number/string/operator/comment
- Bad character

### `JspLiteCompletionContributor.java`

기본 자동완성을 제공합니다.

현재 제공:

- JSP directive snippet
- directive attribute
- JSTL core tag
- JSTL fmt tag
- JSP action tag
- EL implicit object
- EL operator
- HTML/JSP attribute
- JSP snippet

특징:

- 파일 앞부분에서 `taglib` directive를 읽어 `c`, `fmt` prefix 변경을 반영합니다.
- scan limit은 `64_000`자로 제한되어 있습니다.

### `JspLiteReferenceContributor.java`

PSI reference 기반 이동을 제공합니다.

현재 처리:

- `<%@ include file="..." %>`
- `<jsp:include page="..." />`
- `<jsp:forward page="..." />`
- JSP 파일 안의 JavaScript 함수 호출에서 함수 선언으로 이동
- HTML event attribute 안의 JavaScript 호출 이동

include path 해석 순서:

- 상대 경로는 현재 JSP 파일의 parent 기준
- `/...` 경로는 `webapp`, `WebContent`, `web` 폴더 기준 탐색
- 실패 시 project base dir 기준 탐색

### `JspLiteGotoDeclarationHandler.java`

Ctrl+Click/Go to Declaration 보강용 핸들러입니다.

현재 처리:

- JSP 내부 JS 함수 선언 이동
- 변수 선언 이동
- 함수 파라미터 이동
- JSTL `var="..."`로 선언된 변수 이동
- `object.method()` 형태의 메서드 선언 이동
- `<script src="...">`로 연결된 외부 `.js` 파일 검색
- 프로젝트 내 `.js` 파일 suffix 검색

주의할 점:

- JavaScript parser가 아니라 regex 기반입니다.
- bundled/minified JS나 동적 script path에서는 정확도가 낮을 수 있습니다.

### `JspLiteCommenter.java`

주석 토글을 담당합니다.

현재 동작:

- `<script>` 내부 line comment: `//`
- `<script>` 내부 block comment: `/* ... */`
- 그 외 영역 line/block comment: `<!-- ... -->`
- commented block comment prefix/suffix: `<%--`, `--%>`

## 빌드 방법

저장소 루트에서 실행:

```powershell
.\gradlew.bat -p jsp-lite-intellij-plugin buildPlugin
```

플러그인 모듈 안에서 실행할 경우:

```powershell
..\gradlew.bat buildPlugin
```

생성물:

```text
jsp-lite-intellij-plugin\build\distributions\jsp-lite-intellij-plugin-0.1.6.zip
```

## 로컬 환경 의존성

`gradle.properties`에 로컬 경로가 들어있습니다.

```properties
org.gradle.java.home=D:/jsProject/zulu21
org.gradle.java.installations.paths=D:/jsProject/zulu21
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8 -Djavax.net.ssl.trustStore=D:/intelliJSSL/jssecacerts -Djavax.net.ssl.trustStorePassword=changeit
```

`build.gradle.kts`는 IntelliJ Community local installation을 사용합니다.

```kotlin
intellijPlatform {
    local("D:/IntelliJ IDEA Community Edition")
}
```

다른 PC에서 작업할 때는 위 경로를 먼저 맞춰야 합니다.

## AI에게 작업시킬 때 추천 프롬프트

```text
이 프로젝트는 IntelliJ IDEA Community용 JSP Lite 플러그인이다.
Ultimate JSP 지원을 복제하지 말고, lightweight lexer/regex 기반 편의 기능을 유지해라.
색상은 단일 고정 RGB를 쓰지 말고 JBColor(light, dark)와 TextAttributesKey fallback을 써라.
기능 추가 전 README.md와 VIBE_CODING.md를 읽고 기존 구조를 따라라.
변경 후 ./gradlew.bat -p jsp-lite-intellij-plugin buildPlugin로 빌드 검증해라.
```

## 작업할 때 조심할 점

- `JspLiteLexer`에 기능을 추가할 때 상태 전환을 반드시 확인합니다.
- `<script>`, `<style>`, HTML attribute 내부의 JSP/EL 처리를 깨지 않도록 합니다.
- regex 기반 navigation은 너무 공격적으로 만들면 오탐이 늘어납니다.
- include path 해석은 Windows 경로와 web root 경로를 모두 고려해야 합니다.
- Color Scheme 관련 변경에서 다크 테마 기준 단일 `new Color(...)`를 다시 넣지 않습니다.
- README의 ZIP 버전과 `build.gradle.kts`의 `version`을 함께 맞춥니다.

## 다음에 하기 좋은 개선 후보

- `fn:` JSTL functions 자동완성 추가
- JSTL sql/xml prefix 자동완성 옵션 추가
- `<jsp:param name="" value="" />` 같은 tag별 attribute tail 개선
- `.tag` 파일에서 tag directive 자동완성 강화
- include path 자동완성
- `<link href="...">`, `<script src="...">` 경로 이동 추가
- CSS class/id에서 HTML 사용처 이동
- JS 함수 탐색 regex 테스트 케이스 추가
- README에 스크린샷 추가

## 현재 한계

- JSP 문법 검증을 하지 않습니다.
- Java import/type/method resolution을 하지 않습니다.
- TLD 파일을 읽어 taglib를 완전 분석하지 않습니다.
- JavaScript AST를 만들지 않습니다.
- 동적 include, 동적 script src, 복잡한 EL은 제한적으로만 인식합니다.

## 현재 상태 체크리스트

- [x] 지원 확장자 등록
- [x] syntax highlighter 등록
- [x] color settings page 등록
- [x] commenter 등록
- [x] completion contributor 등록
- [x] reference contributor 등록
- [x] goto declaration handler 등록
- [x] 0.1.6 테마 색상 개선
- [x] `jsp-lite-intellij-plugin-0.1.6.zip` 빌드 완료
