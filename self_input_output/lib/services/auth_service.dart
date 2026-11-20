import 'package:firebase_auth/firebase_auth.dart';
import '../models/app_user.dart';
import 'user_service.dart';

class AuthService {
  final _auth = FirebaseAuth.instance;
  final _userService = UserService();

  User? get currentUser => _auth.currentUser;
  Stream<User?> get authStateChanges => _auth.authStateChanges();

  Future<void> signIn({required String email, required String password}) {
    return _auth.signInWithEmailAndPassword(email: email, password: password);
  }

  Future<void> register({
    required String fullName,
    required String email,
    required String password,
  }) async {
    final credential = await _auth.createUserWithEmailAndPassword(
      email: email,
      password: password,
    );
    final user = credential.user;
    if (user == null) return;

    await user.updateDisplayName(fullName);
    await user.reload();

    await _userService.createUser(
      AppUser(uid: user.uid, fullName: fullName, email: email),
    );
  }

  Future<void> signOut() => _auth.signOut();

  // Firebase-ийн алдааны кодыг монгол мессеж болгоно
  static String errorMessage(FirebaseAuthException e) {
    switch (e.code) {
      case 'invalid-email':
        return 'Имэйл хаяг буруу байна';
      case 'user-disabled':
        return 'Энэ хэрэглэгчийн эрх хаагдсан байна';
      case 'user-not-found':
      case 'wrong-password':
      case 'invalid-credential':
        return 'Имэйл эсвэл нууц үг буруу байна';
      case 'email-already-in-use':
        return 'Энэ имэйл аль хэдийн бүртгэлтэй байна';
      case 'weak-password':
        return 'Нууц үг хэт хялбар байна';
      case 'too-many-requests':
        return 'Олон удаа оролдлоо. Түр хүлээгээд дахин оролдоно уу';
      case 'network-request-failed':
        return 'Интернэт холболтоо шалгана уу';
      default:
        return e.message ?? 'Алдаа гарлаа (${e.code})';
    }
  }
}
