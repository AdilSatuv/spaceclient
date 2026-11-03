# Единая орбитальная тема меню · 3 ноября 2026

По поручению пользователя обновлены фон и кнопки всех семи категорий Space, настроек/косметики/редактора, а также основных штатных меню Minecraft. Это законченный локальный срез после второй волны аккаунтов; push/main/deploy не выполнялись.

## Изменение

`MenuBackdrop` рисует одну статичную сцену: bundled landscape, тёмно-синяя подложка, звёзды и процедурная планета с орбитами. Сетевых ресурсов, видео и фонового таймера нет. Редактор HUD получил отдельный лёгкий overlay над настоящим миром для точного размещения. Ранний запуск использует обновлённую базовую палитру без обращения к ещё не готовым игровым текстурам.

`MenuTheme` объединяет панели, карточки и кнопки: скругление, синие поверхности, золотой акцент, отчётливые hover/keyboard focus/disabled/selected. На общий painter переведены каталог Grid/List, все его категории и вкладки, settings shell, косметика, HUD inspector и старый экран Mods. Переключатели/ползунки также обновлены. В компактном RU списке состояние выровнено перед звездой без наложения на название.

Штатные кнопки и три типа ползунков используют новую отрисовку с сохранёнными native callbacks. Логика нормализации option/volume, listener и release не скопирована. Отрисовка старого прямоугольного handle подавляется временным пустым scissor только во время native drag callback, затем прежний scissor восстанавливается. Difficulty lock получил собственную тематическую геометрию.

Native списки миров/серверов/языков/клавиш/resource packs используют общую полупрозрачную подложку. Для строк добавлен настоящий scissor по native bounds с пересечением существующей области, вместо прежнего скрытия переполнения непрозрачным dirt mask. Отдельный `renderDirtBackground` также перехвачен для resource packs. Действия, выбор, scrollbars и содержимое списков сохранены.

## Проверка и исправления

Свежие **300 Java +155 launcher =455 тестов**, launcher types, game/strict launcher build и полный format проходят. Обычные general/API тесты не перезапускались: соответствующие исходники не менялись, их 75 проверок относятся к предыдущей принятой волне 530.

Финальный run `c6e95b3f-6583-4205-aa4e-a7dd917a96b3`: **58 PNG,3 actual native slider drag/release checks,6 keyboard handler steps,exit0,GL errors0**, без ERROR/FATAL в журнале. EN1280×720/GUI2, RU854×480/GUI2 и RU1280×720/GUI3. Проверены title/focus, settings/video/controls/language/sounds/skin/chat/resources, worlds/create/flat/custom presets, server forms, confirmation, Grid/List и 7 категорий, settings tabs, cosmetics, pause/lock, LAN form, achievements/statistics, editor/arrange, old Mods и waypoints. Восемь ключевых финальных кадров просмотрены полноразмерно; другие кандидаты также осматривались при устранении дефектов. Это 58 состояний/кадров, не 58 уникальных экранов.

Native проверка обнаружила missing inherited shadow fields у LockButton: исправлено настоящим superclass ButtonWidget. После просмотра кадров исправлены перекрывающий scene dirt entry point, строки под footer при прозрачных masks и старые pixels у краёв sliders. Ошибочные/промежуточные попытки сохранены в ignored reports; за финальную приёмку не выдаются.

Helper находится в `scripts/qa/menu-theme`, не входит в production JAR. Каждый run имеет новый private runtime/config/world, hardlink только immutable cache, невидимый Canvas, без OS input. Только в QA отключены constructor/start LAN detector, чтобы пустой server list не открывал multicast socket. Нет публикации мира в LAN или входа на внешний сервер. Physical input, assistive technology, third-party/Realms/streaming и полная resource/shader/DPI матрица здесь не приняты.

## Точная сборка

Game tree `646284f248b09eee824949dcee60433b9df1c2dd`, ID **dev-646284f248b0**, размер **1152021bytes**, SHA-256 **7b84aa7030f5b208f244114312870a0f12191fcc57f7d5b6d6ea5373dfe462e5**. Все 47 точных mixins и новый MenuSlider обязательны в bundle; positive/omission fixtures синхронизированы. Четыре новых production mixins: MenuOptionSliderMixin/MenuSoundSliderMixin/MenuLockMixin/MenuListMixin.

Source commits96f33d5/0a7e7da/b39bf9f/3736c6d/db482b4; QA helper7d47e0a. [QA JSON](qa/menu-theme-2026-11-03.json). Финальные logs — `.reports/team-review/menu-theme-final-product.log`, `menu-theme-final-native.log`, `menu-theme-launcher-tests.log`, `menu-theme-bundle.log`, `menu-theme-bundle-identity.log`, `menu-theme-final-format.log`. Native raw directory указан в QA JSON, launcher identity5groups pass на новом pin.

Тот же JAR включён в `apps/launcher/dist/game`; пользовательский instance автоматически не заменялся. Посторонний активный Java и видимый launcher30140 сохранены. После завершения игры нужно перезапустить лаунчер, нажать «Подготовить файлы» и запустить Minecraft. Миры, nickname/profile и Java settings пользователя не менялись. Сайт/API остаются 50566/50565.

## Что дальше

[Актуальные приоритеты по коду и проверкам](CURRENT-PRIORITIES-2026-11-03.md): HUD performance → clean Windows/package lifecycle и app upgrade/uninstall → Space password/session/recovery/SQLite restore → physical/multiplayer matrix → единая навигация и полный RU. Уже реализованные аккаунты/backup/JAR rollback не объявляются незавершёнными.

Прежние performance observations относятся к старому game tree и не выдаются за новый benchmark темы. Microsoft/public hosting остаются отложенными. Codebase Memory недоступна (`Transport closed`); read-only аудит выполнен по исходникам. Для UI использованы прочитанные правила `ui-ux-pro-max` о контрасте, фокусе и состояниях controls; локальный Python-поиск не запускался успешно, проверенный database match не заявляется.
