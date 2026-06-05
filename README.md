# JSP Lite for IntelliJ IDEA Community

JSP Lite는 IntelliJ IDEA Community Edition에서 JSP 파일을 조금 더 편하게 읽고 편집하기 위한 경량 플러그인입니다.

IntelliJ IDEA Community Edition은 Ultimate Edition처럼 JSP/Java EE 지원을 제공하지 않습니다. 이 플러그인은 완전한 JSP 런타임이나 Java EE 프로젝트 모델을 구현하지 않고, 커뮤니티 버전에서 특히 부족한 에디터 경험에 집중합니다.

## 지원 파일

- `.jsp`
- `.jspf`
- `.jspx`
- `.tag`
- `.tagx`

## 주요 기능

- JSP directive, scriptlet, declaration, expression, JSP comment 하이라이팅
- HTML tag, attribute, string, comment, doctype 하이라이팅
- EL expression 하이라이팅 및 기본 EL 키워드 인식
- `<script>` 안의 JavaScript 유사 코드 하이라이팅
- `<style>` 안의 CSS 유사 코드 하이라이팅
- JSP/HTML/JavaScript 문맥별 주석 처리
- JSP directive, JSTL tag, EL implicit object, 자주 쓰는 attribute 자동완성
- 선언된 JSTL prefix를 읽어서 `c`, `fmt` 외 커스텀 prefix 자동완성에 반영
- `<%@ include file="..." %>`, `<jsp:include page="..." />`, `<jsp:forward page="..." />` 경로 이동
- JSP 내부 JavaScript 함수/변수 이동
- `<script src="...">`로 연결된 JavaScript 파일의 함수 선언 이동
- `Settings > Editor > Color Scheme > JSP Lite` 색상 설정 페이지 제공

## 0.1.6 변경점

- 다크 테마 기준 단일 RGB 색상을 제거했습니다.
- 토큰별 구분감은 유지하면서 라이트/다크 테마별 기본색을 `JBColor`로 분리했습니다.
- 각 색상 키는 IntelliJ 기본 `DefaultLanguageHighlighterColors` fallback에도 연결했습니다.
- JavaScript property 색상을 낮추고 dot 앞 변수 색상을 강조해 `item.apprDivision` 같은 코드의 가독성을 개선했습니다.
- 외부 JS 함수/변수 이동 탐색은 유지하되, 검색 시간이 5초를 넘으면 중단하도록 조정했습니다.

## 설치

빌드된 ZIP 파일을 IntelliJ IDEA Community Edition에 직접 설치합니다.

```text
jsp-lite-intellij-plugin\build\distributions\jsp-lite-intellij-plugin-0.1.6.zip
```

설치 순서:

1. IntelliJ IDEA Community Edition을 엽니다.
2. `File > Settings > Plugins`로 이동합니다.
3. 톱니바퀴 아이콘을 누릅니다.
4. `Install Plugin from Disk...`를 선택합니다.
5. `jsp-lite-intellij-plugin\build\distributions\jsp-lite-intellij-plugin-0.1.6.zip`을 선택합니다.
6. IntelliJ를 재시작합니다.

`.jsp` 파일이 여전히 plain text나 다른 파일 형식으로 열린다면 `Settings > Editor > File Types`에서 `*.jsp` 등록을 확인하세요. 잘못된 파일 형식에 등록되어 있으면 제거한 뒤 `JSP Lite`에 다시 추가하면 됩니다.

## 색상 설정

JSP Lite는 IntelliJ의 Color Scheme 시스템을 사용합니다.

설정 위치:

```text
Settings > Editor > Color Scheme > JSP Lite
```

색상이 마음에 들지 않으면 여기서 JSP directive, JSP expression, HTML tag, code variable, code parameter, code property, CSS value, code comment 등을 직접 조정할 수 있습니다.

## 빌드

저장소 루트에서 실행합니다.

```powershell
.\gradlew.bat -p jsp-lite-intellij-plugin buildPlugin
```

생성 위치:

```text
jsp-lite-intellij-plugin\build\distributions\
```

현재 버전의 설치 파일:

```text
jsp-lite-intellij-plugin\build\distributions\jsp-lite-intellij-plugin-0.1.6.zip
```

## 개발 환경

- Java toolchain: 21
- Java compile target: 11
- Gradle IntelliJ Platform Plugin 사용
- IntelliJ Platform local path: `D:/IntelliJ IDEA Community Edition`

`jsp-lite-intellij-plugin/gradle.properties`에서 로컬 JDK와 trustStore 경로를 사용합니다. 다른 PC에서 빌드한다면 해당 경로를 환경에 맞게 바꿔야 합니다.

## 현재 범위

JSP Lite가 목표로 하는 범위:

- JSP 파일을 Community Edition에서 JSP답게 열기
- JSP, HTML, EL, script, style이 섞인 파일의 가독성 개선
- 오래된 JSP 프로젝트에서 자주 필요한 include 이동과 기본 자동완성 제공
- 복잡한 IDE 통합보다 빠르고 가벼운 편집 경험 유지

구현하지 않는 범위:

- IntelliJ IDEA Ultimate 수준의 JSP PSI
- Servlet/Jakarta EE 프로젝트 모델 통합
- Java type resolution
- Java refactoring inside JSP scriptlet
- JSP inspection
- JSP 컴파일 또는 런타임 검증
- TLD 기반 완전한 taglib 분석

## 구조

```text
src/main/java/com/github/codex/jsplite/
  JspLiteLanguage.java
  JspLiteFileType.java
  JspLiteParserDefinition.java
  lexer/
    JspLiteLexer.java
    JspLiteTokenTypes.java
  highlighting/
    JspLiteSyntaxHighlighter.java
    JspLiteColorSettingsPage.java
    JspLiteSyntaxHighlighterFactory.java
  completion/
    JspLiteCompletionContributor.java
  editor/
    JspLiteCommenter.java
  navigation/
    JspLiteReferenceContributor.java
    JspLiteGotoDeclarationHandler.java
```

## 주의사항

이 플러그인은 실용적인 lightweight lexer와 정규식 기반 탐색을 사용합니다. 큰 JSP 파일이나 복잡한 동적 include, 복잡한 JavaScript 번들 구조에서는 Ultimate Edition 수준의 정확도를 기대하면 안 됩니다.

대신 Community Edition에서 JSP를 plain text로 보는 상황을 피하고, 레거시 JSP 프로젝트를 빠르게 읽고 수정하는 데 초점을 맞춥니다.
