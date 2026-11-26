# `lib/` доторх функцуудын тайлбар

**Төсөл:** `self_input_output` — Flutter + Firebase (Auth, Cloud Firestore) дээр бичсэн орлого/зарлагын түрийвч апп.

Баримт хоёр хэсэгтэй. Функц бүр **зөвхөн нэг удаа** тайлбарлагдсан (олон газар дуудагдаж байвал "хаана ашиглагдаж буй" гэдгийг нь тэмдэглэв). UI зурдаг `build()`-ийн дотор талын render код (Container, Row, Text г.м.) тайлбарт **ороогүй**.

| Хэсэг | Агуулга |
|---|---|
| **1-р хэсэг** | Төсөл дотор өөрөө бичсэн функцууд |
| **2-р хэсэг** | Flutter / Dart / Firebase-ийн бэлэн функцууд (төсөлд дуудаж ашигласан) |

**Тэмдэглэгээ:** `*` = заавал өгөх параметр (required). Параметр нь `{}` дотор бол нэрлэсэн (named) параметр.

---

# 1-р хэсэг. Өөрөө бичсэн функцууд

## 1.1 `main.dart`

### `main()`
- **Параметр:** байхгүй
- **Буцаах:** `Future<void>` (`async`)
- **Логик:** Апп-ын эхлэх цэг.
  1. `WidgetsFlutterBinding.ensureInitialized()` дуудна. Flutter engine бэлэн болтол хүлээнэ (`await` хийхээс өмнө заавал хэрэгтэй).
  2. `Firebase.initializeApp(options: DefaultFirebaseOptions.currentPlatform)`-ээр Firebase-тэй холбогдоно. `try/catch` дотор байгаа тул алдаа гарсан ч апп унахгүй, зөвхөн `print` хийнэ.
  3. `runApp(const MyApp())`-ээр апп-ыг ажиллуулна.

### `AuthGate.build(BuildContext context)`
- **Параметр:** `context`* (Flutter өөрөө дамжуулна)
- **Буцаах:** `Widget`
- **Логик:** Render хийдэг ч дотор нь нэвтрэлтийн чиглүүлэх логик байгаа тул энд бичлээ. `FirebaseAuth.instance.authStateChanges()` stream-ийг `StreamBuilder`-ээр сонсож 4 тохиолдлыг шалгана (дарааллаар):
  1. `connectionState == waiting` → ачаалж буй спиннер.
  2. `snapshot.hasError` → алдааны текст.
  3. `snapshot.hasData && data != null` (нэвтэрсэн) → `MainPage`.
  4. Бусад (нэвтрээгүй) → `OnboardingScreen`.

  Stream тул нэвтэрсэн/гарсан үед дэлгэц **автоматаар** солигдоно.

---

## 1.2 `utils/formatters.dart`

### `formatMoney(double value)`
- **Параметр:** `value`* — мөнгөн дүн
- **Буцаах:** `String` — мянгатын таслалтай бүхэл тоо. Жишээ: `2000000` → `"2,000,000"`
- **Логик:** `toStringAsFixed(0)`-ээр аравтын хэсгийг хасч бүхэл тоо болгоно. Дараа нь regex `\B(?=(\d{3})+(?!\d))`-ээр "араас нь яг 3-ийн үржвэр цифр үлдэж байгаа" байрлал бүрт `,` оруулна.

### `formatDate(DateTime d)`
- **Параметр:** `d`* — огноо
- **Буцаах:** `String` — `YYYY-MM-DD`. Жишээ: `"2026-11-20"`
- **Логик:** Сар, өдрийг `padLeft(2, '0')`-ээр 2 оронтой болгож (`5` → `05`), `'${d.year}-$m-$day'` хэлбэрээр нэгтгэнэ.

---

## 1.3 `models/` — өгөгдлийн загварууд

Загвар бүр Firestore-ийн document ↔ Dart объект хөрвүүлэлт хийнэ.

### `AppUser` (`app_user.dart`)
Талбарууд: `uid`*, `fullName`*, `email`*, `createdAt` (заавал биш, `DateTime?`).

| Функц | Параметр | Буцаах | Логик |
|---|---|---|---|
| `AppUser({...})` (const constructor) | `uid`*, `fullName`*, `email`*, `createdAt` | `AppUser` | Өөрчлөгдөхгүй (immutable) объект үүсгэнэ. |
| `AppUser.fromMap(Map<String,dynamic> data)` (factory) | `data`* — Firestore-оос ирсэн map | `AppUser` | Талбар бүрийг `data['x'] ?? ''`-ээр уншина (байхгүй бол хоосон мөр). `createdAt` нь `Timestamp` төрлийн бол `.toDate()`-ээр `DateTime` болгоно, үгүй бол `null`. |
| `toMap()` | байхгүй | `Map<String,dynamic>` | Firestore-д хадгалах map үүсгэнэ. `createdAt`-д `FieldValue.serverTimestamp()` тавина, өөрөөр хэлбэл цагийг төхөөрөмж биш **серверийн цагаар** хадгална. |

### `TransactionItem` (`transaction_item.dart`)
Талбарууд: `id`*, `title`*, `date`*, `amount`*, `isIncome`* (`true` = орлого, `false` = зарлага).

| Функц | Параметр | Буцаах | Логик |
|---|---|---|---|
| `TransactionItem({...})` | бүх 5 талбар* | `TransactionItem` | const constructor. |
| `TransactionItem.fromDoc(QueryDocumentSnapshot<Map<String,dynamic>> doc)` (factory) | `doc`* — Firestore document | `TransactionItem` | `id`-г `doc.id`-аас (document-ийн нэр) авна. `amount`-ыг `(data['amount'] ?? 0).toDouble()`-оор уншина. Firestore `int` хадгалсан байж болох тул `double` болгож тулгана. `isIncome` байхгүй бол `false`. `date` нь `Timestamp` бол `toDate()`, үгүй бол `DateTime.now()`. |
| `toMap()` | байхгүй | `Map<String,dynamic>` | `title`, `amount`, `isIncome`, `date` (`Timestamp.fromDate`)-ийг буцаана. **`id` орохгүй**, учир нь document-ийн нэр нь id юм. |

### `PendingPayment` (`pending_payment.dart`)
Талбарууд: `id`*, `title`*, `amount`*, `dueDate`*, `iconKey` (өгөхгүй бол `'other'`).

| Функц | Параметр | Буцаах | Логик |
|---|---|---|---|
| `PendingPayment({...})` | `id`*, `title`*, `amount`*, `dueDate`*, `iconKey` | `PendingPayment` | const constructor. |
| `icon` (getter) | байхгүй | `IconData` | Firestore-д зөвхөн текст түлхүүр (`youtube`, `electricity`, `rent`, `music`, `other`) хадгалдаг. Энэ getter нь `_icons` map-аас харгалзах `Icons.*`-ийг олно. Түлхүүр олдохгүй бол `Icons.receipt_long`. |
| `PendingPayment.fromDoc(QueryDocumentSnapshot<Map<String,dynamic>> doc)` (factory) | `doc`* | `PendingPayment` | `TransactionItem.fromDoc`-тай ижил зарчим: `id = doc.id`, `amount`-ыг `double` болгох, `dueDate`-ийг `Timestamp → DateTime` (олдохгүй бол одоогийн цаг), `iconKey` байхгүй бол `'other'`. |
| `toMap()` | байхгүй | `Map<String,dynamic>` | `title`, `amount`, `dueDate` (`Timestamp.fromDate`), `iconKey`. `id` орохгүй. |

---

## 1.4 `services/` — Firebase-тэй харьцах давхарга

Firestore бүтэц:
```
users/{uid}                      ← AppUser
users/{uid}/transactions/{id}    ← TransactionItem
users/{uid}/pending_payments/{id}← PendingPayment
```

### `AuthService` (`auth_service.dart`)
Дотроо `FirebaseAuth.instance` болон `UserService`-ийг барина.

| Функц | Параметр | Буцаах | Логик |
|---|---|---|---|
| `currentUser` (getter) | байхгүй | `User?` | Одоо нэвтэрсэн хэрэглэгч, байхгүй бол `null`. |
| `authStateChanges` (getter) | байхгүй | `Stream<User?>` | Нэвтрэх/гарах бүрд шинэ утга дамжуулах stream. |
| `signIn({email*, password*})` | `email`*, `password`* | `Future<void>` | `signInWithEmailAndPassword`-г шууд дамжуулна. Алдаа гарвал `FirebaseAuthException` шидэгдэх бөгөөд дуудсан талд баригдана. |
| `register({fullName*, email*, password*})` | `fullName`*, `email`*, `password`* | `Future<void>` | 1) `createUserWithEmailAndPassword`-ээр Auth бүртгэл үүсгэнэ. 2) `credential.user` `null` бол зогсоно. 3) `updateDisplayName(fullName)`-ээр нэрийг Auth профайлд хадгална. 4) `reload()`-оор локал `User` объектыг шинэчилнэ (тэгэхгүй бол `displayName` хуучин `null` хэвээр үлдэнэ). 5) `UserService.createUser(...)`-ээр Firestore-ын `users/{uid}`-д бичнэ. |
| `signOut()` | байхгүй | `Future<void>` | Системээс гаргана. |
| `errorMessage(FirebaseAuthException e)` (static) | `e`* | `String` | `e.code`-оор switch хийж монгол мессеж буцаана (хүснэгтийг доор харна уу). |

`errorMessage`-ийн кодын жагсаалт:

| Firebase код | Харуулах мессеж |
|---|---|
| `invalid-email` | Имэйл хаяг буруу байна |
| `user-disabled` | Энэ хэрэглэгчийн эрх хаагдсан байна |
| `user-not-found` / `wrong-password` / `invalid-credential` | Имэйл эсвэл нууц үг буруу байна |
| `email-already-in-use` | Энэ имэйл аль хэдийн бүртгэлтэй байна |
| `weak-password` | Нууц үг хэт хялбар байна |
| `too-many-requests` | Олон удаа оролдлоо. Түр хүлээгээд дахин оролдоно уу |
| `network-request-failed` | Интернэт холболтоо шалгана уу |
| бусад | `e.message`, байхгүй бол `Алдаа гарлаа (код)` |

### `UserService` (`user_service.dart`)
| Функц | Параметр | Буцаах | Логик |
|---|---|---|---|
| `createUser(AppUser user)` | `user`* | `Future<void>` | `users` collection-ы `user.uid` нэртэй document-д `user.toMap()`-ийг `set` хийнэ. Document-ийн нэр нь Auth-ын uid-тай таарна. |
| `getUser(String uid)` | `uid`* | `Future<AppUser?>` | `users/{uid}`-г `get()`-ээр уншина. `data()` `null` бол `null`, үгүй бол `AppUser.fromMap`. |

### `TransactionService` (`transaction_services.dart`)
| Функц | Параметр | Буцаах | Логик |
|---|---|---|---|
| `TransactionService(String uid)` | `uid`* | — | Хэрэглэгчийн uid-г хадгална. Дотоод `_col` getter нь `users/{uid}/transactions` collection-г өгнө. |
| `watchTransactions()` | байхгүй | `Stream<List<TransactionItem>>` | `date`-ээр **буурахаар** (шинэ нь дээр) эрэмбэлж, `snapshots()`-аар бодит цагийн stream авна. Өгөгдөл өөрчлөгдөх бүрд `docs`-ийг `TransactionItem.fromDoc`-оор жагсаалт болгоно. |
| `addTransaction(TransactionItem item)` | `item`* | `Future<void>` | `_col.add(item.toMap())`. Document-ийн id-г Firestore автоматаар үүсгэнэ. |
| `deleteTransaction(String id)` | `id`* | `Future<void>` | `_col.doc(id).delete()`. |

### `PendingPaymentService` (`pending_payment_service.dart`)
Дотоод getter-үүд: `_userDoc` (`users/{uid}`), `_pending` (`.../pending_payments`), `_transactions` (`.../transactions`).

| Функц | Параметр | Буцаах | Логик |
|---|---|---|---|
| `PendingPaymentService(String uid)` | `uid`* | — | uid хадгална. |
| `watchPending()` | байхгүй | `Stream<List<PendingPayment>>` | `dueDate`-ээр **өсөхөөр** (хугацаа ойр нь эхэнд) эрэмбэлж бодит цагт өгнө. |
| `addPending(PendingPayment p)` | `p`* | `Future<void>` | `_pending.add(p.toMap())`. |
| `pay(PendingPayment p)` | `p`* (`id` нь Firestore-д байгаа document-ийн id байх ёстой) | `Future<void>` | Нэг **Firestore transaction** дотор: 1) `tx.get`-ээр pending document-ийг уншина. 2) Байхгүй бол (аль хэдийн төлөгдсөн) юу ч хийхгүй буцна, давхар төлөлтөөс сэргийлнэ. 3) Ижил `title`, `amount`-тай, `date = DateTime.now()`, `isIncome=false` зарлагын `TransactionItem` үүсгэж `tx.set`-ээр шинэ document-д бичнэ. 4) `tx.delete`-ээр pending-ийг устгана. Transaction учраас "зарлага нэмэгдсэн ч pending үлдсэн" гэсэн дундын төлөв үүсэхгүй: бүгд амжилттай эсвэл юу ч өөрчлөгдөхгүй. |

---

## 1.5 `Screens/` — дэлгэц доторх логик функцууд

### `_SplashScreenState.initState()` (`splash_screen.dart`)
- **Параметр:** байхгүй
- **Буцаах:** `void`
- **Логик:** `super.initState()`-ийн дараа 3 секундийн `Timer` тавьж, дуусахад `Navigator.pushReplacementNamed(context, '/auth')`-аар `AuthGate` руу шилжинэ. Replacement тул Splash руу буцаж болохгүй.

### `_LoginScreenState._submit()` (`login_screen.dart`)
- **Параметр:** байхгүй (`_email`, `_password` controller-оос унших)
- **Буцаах:** `Future<void>`
- **Логик:**
  1. Имэйл, нууц үгийг `trim()`-ээр цэвэрлэнэ.
  2. Аль нэг нь хоосон бол `_errorMessage` тавиад зогсоно.
  3. `_isLoading = true`, алдааг цэвэрлэнэ.
  4. `AuthService.signIn(...)` дуудна. Амжилттай бол `/homepage` руу бүх өмнөх дэлгэцийг устгаад шилжинэ.
  5. `FirebaseAuthException` → `AuthService.errorMessage(e)`, бусад алдаа → `'$e'` гэж харуулна.
  6. `finally`-д `_isLoading = false`.

### `_RegisterScreenState._register()` (`register_screen.dart`)
- **Параметр:** байхгүй (4 controller-оос унших: нэр, имэйл, нууц үг, нууц үг давтах)
- **Буцаах:** `Future<void>`
- **Логик:** `_submit()`-тэй бараг ижил урсгалтай, гэхдээ Firebase руу явахаас өмнө 3 шалгалт хийнэ:
  1. Нэр / имэйл / нууц үг хоосон → "Талбаруудаа бүрэн бөглөнө үү".
  2. Нууц үг 8 тэмдэгтээс богино → "хамгийн багадаа 8 тэмдэгт".
  3. Нууц үг ≠ давтсан нууц үг → "таарахгүй байна".

  Тэгээд `AuthService.register(...)` дуудаж, амжилттай бол `/homepage` руу шилжинэ. Алдаа, `finally` боловсруулалт `_submit`-тэй адил.

### `HomeHeader._greetingByTime()` (`homepage.dart`)
- **Параметр:** байхгүй
- **Буцаах:** `String`
- **Логик:** `DateTime.now().hour`-оор: 12-оос өмнө "Өглөөний мэнд?", 18-аас өмнө "Өдрийн мэнд?", үлдсэн нь "Оройн мэнд?".

### `_WalletScreenState._pay(PendingPayment p)` (`wallet_screen.dart`)
- **Параметр:** `p`* — төлөх гэж буй pending төлбөр
- **Буцаах:** `Future<void>`
- **Логик:**
  1. `_payingIds` (Set) дотор `p.id` байвал шууд буцна. Давхар дарахаас хамгаална.
  2. `_payingIds.add(p.id)`. "Төлөх" товч түр идэвхгүй болно (`onPay` нь `null` болдог).
  3. `PendingPaymentService.pay(p)` дуудна.
  4. Амжилттай бол SnackBar "… төлөгдлөө", алдаа гарвал "Төлөхөд алдаа гарлаа: …".
  5. `finally`-д id-г Set-ээс хасна.

  Бүх алхамд `mounted` шалгаж, дэлгэц хаагдсан бол UI-д хүрэхгүй.

### Callback-д суулгасан жижиг логикууд
| Байршил | Үйлдэл |
|---|---|
| `MainPage` → `NavigationBar.onDestinationSelected(index)` | `setState(() => _currentIndex = index)`. `IndexedStack` харгалзах tab-ыг үзүүлнэ, tab солиход scroll зэрэг төлөв хадгалагдана. |
| `MainPage` → `WalletScreen(onBack: ...)` | `_currentIndex = 0` болгож нүүр tab руу буцаана. |
| `WalletScreen` → `_SegmentedTabs.onChanged(i)` | `setState(() => _tabIndex = i)`. 0 = Гүйлгээнүүд, 1 = Хүлээгдэж буй. |
| `WalletScreen` → `onBack` сонгох | `widget.onBack` байвал түүнийг, үгүй бөгөөд `Navigator.canPop(context)` бол `Navigator.pop`, бусад үед `null` (буцах сум харагдахгүй). |
| `ProfileScreen` → "Гарах" `onPressed` | `await auth.signOut()`, дараа нь `context.mounted` шалгаад `/login` руу бүх route-ыг устгаж шилжинэ. |

### `StreamBuilder` дотор хийгддэг тооцоолол
- **`HomePage`:** `items`-ээс `isIncome == true`-г `fold`-оор нийлбэрлэж `income`, `false`-г нийлбэрлэж `expense` гаргана. `balance = income - expense`.
- **`WalletScreen`:** `fold`-оор нэг дор `sum + (isIncome ? amount : -amount)` гэж `balance` бодно.

Хоёр газар ижил утга гаргана. Одоогоор хоёр хуулбар байгаа тул дараа нь `services` эсвэл `utils` руу нэг функц болгон гаргаж болно.

---

# 2-р хэсэг. Бэлэн (framework / Firebase) функцууд

## 2.1 Flutter, Dart-ын бэлэн функцууд

### Апп эхлүүлэх
| Функц | Параметр | Буцаах | Логик |
|---|---|---|---|
| `WidgetsFlutterBinding.ensureInitialized()` | байхгүй | `WidgetsBinding` | Flutter-ийн engine ↔ framework холбоог бэлдэнэ. `runApp`-аас өмнө `await` хийх (Firebase гэх мэт) үед заавал дуудна. |
| `runApp(Widget app)` | `app`* — root widget | `void` | Өгсөн widget-ийг дэлгэцний root болгон ажиллуулна. |

### Widget lifecycle (override хийсэн хэсгүүд)
| Функц | Параметр | Логик |
|---|---|---|
| `createState()` | байхгүй | `StatefulWidget`-ийн State объектыг үүсгэнэ (`SplashScreen`, `LoginScreen`, `MainPage` г.м.). |
| `initState()` | байхгүй | State анх үүсэхэд **нэг удаа** дуудагдана. Эхлээд `super.initState()` дуудна. Төсөлд `SplashScreen`-д ашигласан. |
| `dispose()` | байхгүй | Widget устахад дуудагдана. `TextEditingController.dispose()`-оор санах ой чөлөөлнө, дараа нь `super.dispose()`. Төсөлд `LoginScreen`, `RegisterScreen`-д ашигласан. |
| `build(BuildContext context)` | `context`* | UI-г зурж `Widget` буцаана. `setState` эсвэл stream шинэчлэгдэх бүрд дахин дуудагдана. |
| `setState(VoidCallback fn)` | `fn`* | `fn` дотор төлөв өөрчлөөд, Flutter-д "дахин зур" гэж мэдэгдэнэ. `_isLoading`, `_errorMessage`, `_tabIndex`, `_payingIds`, `_currentIndex` өөрчлөхөд ашигласан. |
| `mounted` / `context.mounted` | (getter) | `await`-ийн дараа widget дэлгэц дээр байсаар байгаа эсэхийг шалгана. `false` бол `setState`, `Navigator` дуудаж болохгүй. |

### Навигаци (`Navigator`)
| Функц | Параметр | Логик |
|---|---|---|
| `Navigator.pushReplacementNamed(context, routeName)` | `context`*, `routeName`* | Одоогийн дэлгэцийг шинэ route-ээр **солино**. Splash-д ашигласан. |
| `Navigator.pushNamedAndRemoveUntil(context, routeName, predicate)` | `context`*, `routeName`*, `predicate`* | Шинэ route руу орж, `predicate` нь `true` буцаах хүртэлх өмнөх route-уудыг устгана. Төсөлд `(route) => false` өгсөн тул **бүх** өмнөх дэлгэц устаж буцах товч ажиллахгүй. Onboarding, Login, Register, Profile-д ашигласан. |
| `Navigator.canPop(context)` | `context`* | `bool`. Буцаж гарах дэлгэц stack-д байгаа эсэх. |
| `Navigator.pop(context)` | `context`* | Одоогийн дэлгэцийг хаана. |
| `MaterialApp(routes: {...}, home: ...)` | `routes` (нэр → builder), `home` | Нэрээр нь route дуудах газрын зураг. `/onboarding`, `/auth`, `/login`, `/register`, `/homepage` гэж бүртгэсэн. |

### Хугацаа, stream, мэдэгдэл
| Функц | Параметр | Логик |
|---|---|---|
| `Timer(Duration duration, void Function() callback)` | `duration`*, `callback`* | `duration` өнгөрмөгц `callback`-ийг нэг удаа ажиллуулна (`dart:async`). |
| `StreamBuilder<T>(stream, builder)` | `stream`*, `builder`* | Stream-ийн шинэ утга ирэх бүрд `builder(context, snapshot)`-ийг дахин дуудаж UI шинэчилнэ. `snapshot.connectionState`, `hasError`, `hasData`, `data`, `error` дээр тулгуурлан төлөв тус бүрийг (ачаалж буй, алдаа, өгөгдөлтэй) харуулна. |
| `ScaffoldMessenger.of(context).showSnackBar(SnackBar)` | `context`*, `SnackBar`* | Дэлгэцийн доор түр мэдэгдэл харуулна. |
| `MediaQuery.of(context).padding.top` / `MediaQuery.sizeOf(context)` | `context`* | Status bar-ийн өндөр / дэлгэцийн хэмжээг буцаана. `sizeOf` нь зөвхөн хэмжээ өөрчлөгдөхөд дахин зурна. |

### Controller
| Функц | Логик |
|---|---|
| `TextEditingController()` | Текст талбарын утгыг уншиж/бичих объект. `.text`-ээр утга авна, `dispose()`-оор чөлөөлнө. |

### Dart-ын стандарт функцууд
| Функц | Логик | Хаана |
|---|---|---|
| `double.toStringAsFixed(n)` | `n` оронтой аравтын тэмдэгт мөр болгоно (`0` → бүхэл тоо). | `formatMoney` |
| `String.replaceAllMapped(RegExp, fn)` | Regex таарсан бүх хэсгийг `fn`-ийн үр дүнгээр солино. | `formatMoney` |
| `String.padLeft(width, char)` | Зүүн талаас нөхөж өгөгдсөн урттай болгоно. | `formatDate` |
| `String.trim()` | Хоёр талын хоосон зайг хасна. | `_submit`, `_register` |
| `Iterable.where(test)` | Нөхцөлд таарсан элементүүдийг шүүнэ. | `HomePage` |
| `Iterable.fold<T>(initial, combine)` | Бүх элементийг нэг утга болгон нийлбэрлэнэ. | `HomePage`, `WalletScreen` |
| `Iterable.map(fn).toList()` | Элемент бүрийг хөрвүүлж жагсаалт болгоно. | `watch*` |
| `num.toDouble()` | `int`/`double`-ийг `double` болгоно. | `fromDoc` |
| `DateTime.now()` | Одоогийн огноо, цаг. | `pay`, `_greetingByTime`, `fromDoc` |

---

## 2.2 Firebase-ийн бэлэн функцууд

### `firebase_core`
| Функц | Параметр | Буцаах | Логик |
|---|---|---|---|
| `Firebase.initializeApp({options})` | `options` — `FirebaseOptions` | `Future<FirebaseApp>` | Firebase SDK-г төслийн тохиргоогоор (apiKey, appId, projectId г.м.) эхлүүлнэ. Бусад бүх Firebase дуудлагаас өмнө заавал хийнэ. |
| `DefaultFirebaseOptions.currentPlatform` (`firebase_options.dart`, FlutterFire CLI үүсгэсэн) | (getter) | `FirebaseOptions` | Ажиллаж буй платформоор (`defaultTargetPlatform`) тохирох тохиргоог буцаана. Одоогоор **зөвхөн Android** тохируулагдсан. Web, iOS, macOS, Windows, Linux дээр `UnsupportedError` шидэнэ. |

### `firebase_auth`
| Функц | Параметр | Буцаах | Логик |
|---|---|---|---|
| `FirebaseAuth.instance` | — | `FirebaseAuth` | Нэвтрэлтийн singleton. |
| `authStateChanges()` | байхгүй | `Stream<User?>` | Нэвтрэх/гарах үед `User` эсвэл `null` дамжуулна. `AuthGate` сонсдог. |
| `currentUser` | (getter) | `User?` | Одоогийн хэрэглэгч (`uid`, `email`, `displayName`). |
| `signInWithEmailAndPassword({email, password})` | `email`*, `password`* | `Future<UserCredential>` | Имэйл + нууц үгээр нэвтрүүлнэ. Буруу бол `FirebaseAuthException`. |
| `createUserWithEmailAndPassword({email, password})` | `email`*, `password`* | `Future<UserCredential>` | Шинэ бүртгэл үүсгэж шууд нэвтрүүлнэ. `UserCredential.user` дотор `User` байна. |
| `User.updateDisplayName(String?)` | нэр | `Future<void>` | Auth профайлын харуулах нэрийг шинэчилнэ. |
| `User.reload()` | байхгүй | `Future<void>` | Серверээс хэрэглэгчийн мэдээллийг дахин татаж локал объектыг шинэчилнэ. |
| `signOut()` | байхгүй | `Future<void>` | Гаргана. `authStateChanges` `null` ялгаруулна. |
| `FirebaseAuthException` | — | — | Auth алдаа. `.code` (жишээ: `wrong-password`) болон `.message` талбартай. |

### `cloud_firestore`
| Функц | Параметр | Буцаах | Логик |
|---|---|---|---|
| `FirebaseFirestore.instance` | — | `FirebaseFirestore` | Өгөгдлийн сангийн singleton. |
| `.collection(String path)` | `path`* | `CollectionReference` | Collection-д заана. |
| `.doc([String? id])` | `id` (заавал биш) | `DocumentReference` | Document-д заана. **Id өгөхгүй бол** шинэ санамсаргүй id-тай ref үүснэ (`tx.set(_transactions.doc(), ...)`). |
| `DocumentReference.set(data)` | `data`* | `Future<void>` | Document-ийг бүтнээр нь бичнэ. Байхгүй бол үүсгэнэ, байвал дарж бичнэ. |
| `DocumentReference.get()` | байхгүй | `Future<DocumentSnapshot>` | Document-ийг нэг удаа уншина. `.data()` нь `Map?`, `.exists` нь `bool`, `.id` нь document-ийн нэр. |
| `CollectionReference.add(data)` | `data`* | `Future<DocumentReference>` | Шинэ document-ийг **автомат id**-тай нэмнэ. |
| `DocumentReference.delete()` | байхгүй | `Future<void>` | Document устгана. |
| `Query.orderBy(field, {descending})` | `field`*, `descending` (default `false`) | `Query` | Талбараар эрэмбэлнэ. |
| `Query.snapshots()` | байхгүй | `Stream<QuerySnapshot>` | Өгөгдөл өөрчлөгдөх бүрд шинэ snapshot ялгаруулдаг **бодит цагийн** stream. `.docs` нь `List<QueryDocumentSnapshot>`. |
| `FirebaseFirestore.runTransaction(handler)` | `handler`* — `(Transaction tx) async {...}` | `Future<T>` | Атомар transaction. Handler доторх бүх унших/бичих үйлдэл бүгд амжилттай болох эсвэл огт хэрэгжихгүй. Уншсан document дундаас өөрчлөгдвөл handler-ийг автоматаар дахин ажиллуулна. |
| `Transaction.get(ref)` | `ref`* | `Future<DocumentSnapshot>` | Transaction дотор уншина. Бичихээс өмнө хийх ёстой. |
| `Transaction.set(ref, data)` | `ref`*, `data`* | `Transaction` | Document бичихээр жагсаана. |
| `Transaction.delete(ref)` | `ref`* | `Transaction` | Document устгахаар жагсаана. |
| `Timestamp.fromDate(DateTime)` | `date`* | `Timestamp` | `DateTime` → Firestore-ийн цагийн төрөл. |
| `Timestamp.toDate()` | байхгүй | `DateTime` | Firestore-ийн цаг → `DateTime`. |
| `FieldValue.serverTimestamp()` | байхгүй | `FieldValue` | Бичигдэх үед **серверийн** цагийг автоматаар тавих тэмдэглэгээ. |

---

# Анхаарвал зохих зүйлс (кодыг шалгахад олдсон)

1. **Хавтасны нэрийн том/жижиг үсэг.** `main.dart` дотор `import 'screens/...'` гэж бичсэн боловч хавтас нь `Screens`. macOS дээр ажиллаж байж болох ч Linux, CI дээр алдаа өгнө. Аль нэгийг нь тааруулна уу.
2. **Одоогоор дуудагдаагүй функцууд:** `PendingPaymentService.addPending`, `TransactionService.addTransaction`, `TransactionService.deleteTransaction`, `UserService.getUser`, `AuthService.authStateChanges`. Одоогоор апп "Нэмэх" товчны үйлдэл хийгээгүй тул бэлтгэж байгаа юм шиг байна. `AuthGate` нь `AuthService` биш `FirebaseAuth.instance`-ийг шууд ашиглаж байна.
3. **Валют зөрүүтэй:** `HomePage`, `WalletScreen`-ийн үлдэгдэл `$`-оор, `TransactionTile` нь `₮`-өөр харуулж байна.
4. **`register()`-д rollback байхгүй:** Auth бүртгэл үүссэний дараа Firestore-ийн `createUser` алдаа өгвөл Auth-д хэрэглэгч үлдэнэ, харин `users/{uid}` document байхгүй болно.
5. **`main()`-ийн `catch`:** `Firebase.initializeApp` нь `FirebaseAuthException` шидэхгүй (энэ нь Auth-ийн алдаа) тул тэр `catch` блок хэзээ ч ажиллахгүй. Дараагийн ерөнхий `catch (e)` нь л ажиллана.
