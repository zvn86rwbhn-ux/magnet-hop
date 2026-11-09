# 자석의 모험 · MAGNET HOP

자석의 극성에 따라 점프 높이와 이동 방법이 달라지는 LibGDX 2D 플랫포머입니다. 세 스테이지에서 높은 발판, 천장 가시, 움직이는 자석을 지나 도착 지점까지 이동합니다.

![S 바닥에서 높이 뛰어 다음 발판으로 이동하는 STAGE 1](docs/gameplay.png)

## 게임 실행

[GitHub Releases](https://github.com/zvn86rwbhn-ux/magnet-hop/releases/tag/v3.0.0)에서 운영체제에 맞는 파일을 받으세요. 아래 세 파일에는 Java 실행 환경이 포함되어 있습니다. 게임에는 OpenGL을 지원하는 데스크톱 환경이 필요합니다.

| 운영체제 | 다운로드 | 실행 방법 |
| --- | --- | --- |
| Windows x64 | [설치 파일 (.exe)](https://github.com/zvn86rwbhn-ux/magnet-hop/releases/download/v3.0.0/MagnetHop-Windows-x64.exe) | 설치 후 시작 메뉴에서 MagnetHop 열기 |
| Linux x64 | [AppImage](https://github.com/zvn86rwbhn-ux/magnet-hop/releases/download/v3.0.0/MagnetHop-Linux-x64.AppImage) | 파일 속성에서 실행 권한을 허용한 뒤 열기 |
| Apple Silicon Mac | [디스크 이미지 (.dmg)](https://github.com/zvn86rwbhn-ux/magnet-hop/releases/download/v3.0.0/MagnetHop-macOS-AppleSilicon.dmg) | DMG 안의 `자석의 모험 v3.app` 열기 |

Mac 앱은 Apple Developer ID 서명과 공증을 받지 않아 다운로드한 앱을 macOS가 차단할 수 있습니다. Linux에서 FUSE 사용이 제한되면 실행 권한을 준 뒤 `APPIMAGE_EXTRACT_AND_RUN=1 ./MagnetHop-Linux-x64.AppImage`로 열 수 있습니다.

[공통 실행 ZIP](https://github.com/zvn86rwbhn-ux/magnet-hop/releases/download/v3.0.0/Magnet-Adventure-portable.zip)도 받을 수 있습니다. 이 방식은 Java 17을 설치하고 `Run-Windows.bat`, `Run-Mac.command` 또는 `sh Run-Linux.sh`로 실행합니다. Intel Mac에서는 공통 ZIP과 Intel용 Java 17을 사용하며, Intel Mac 실기 실행은 확인하지 않았습니다.

새 패키지의 타이틀 화면을 Apple Silicon Mac 실기와 Windows Server 2025 x64·Ubuntu 24.04 x64 가상 환경에서 확인했습니다. 같은 게임 JAR로 세 스테이지 완주와 재시작도 확인했습니다.

| 입력 | 동작 |
| --- | --- |
| 좌우 방향키 또는 A / D | 이동 |
| Space | 점프 / 천장 자석에서 내려오기 |
| Esc | 일시정지 / 재개 |
| H | 도움말 |
| R | 현재 스테이지 재시작 |
| Enter | 시작 / 다음 스테이지 / 클리어 후 처음부터 재시작 |
| M | 소리 켜기 / 끄기 |

## 게임 규칙

- 일반 바닥은 기본 점프, 파란 S 바닥은 높은 점프, 빨간 N 바닥은 낮은 점프입니다.
- 머리 N극이 천장 S극에 가까워지면 붙습니다. 움직이는 자석에 매달려 위치를 조절할 수 있습니다.
- 노란 버튼 위로 내려와 착지하면 연결된 바닥의 극이 바뀝니다.
- 가시에 닿거나 아래로 떨어지면 현재 스테이지 처음부터 재시도합니다.

## 소스에서 실행

JDK 17 환경에서 Gradle 래퍼를 사용합니다. Windows에서는 `./gradlew` 대신 `gradlew.bat`을 사용하세요.

```sh
./gradlew :lwjgl3:run
```

```sh
./gradlew :core:verifyGame :core:verifyTiming :core:verifyFeedback :lwjgl3:jar
```

게임 규칙과 스테이지는 `core`, 데스크톱 실행 코드는 `lwjgl3`, 글꼴과 효과음은 `assets`에 있습니다. 사용한 라이브러리와 에셋 출처는 [THIRD-PARTY.md](THIRD-PARTY.md)를 참고하세요.
