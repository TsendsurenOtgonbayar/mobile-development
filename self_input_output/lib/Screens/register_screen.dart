import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

import '../services/auth_service.dart';
import 'auth_widgets.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final _auth = AuthService();
  final _fullName = TextEditingController();
  final _email = TextEditingController();
  final _password = TextEditingController();
  final _secPassword = TextEditingController();
  bool _isLoading = false;
  String? _errorMessage;

  @override
  void dispose() {
    _fullName.dispose();
    _email.dispose();
    _password.dispose();
    _secPassword.dispose();
    super.dispose();
  }

  Future<void> _register() async {
    final fullName = _fullName.text.trim();
    final email = _email.text.trim();
    final password = _password.text.trim();
    final secondPassword = _secPassword.text.trim();

    if (fullName.isEmpty || email.isEmpty || password.isEmpty) {
      setState(() => _errorMessage = 'Талбаруудаа бүрэн бөглөнө үү');
      return;
    }
    if (password.length < 8) {
      setState(() => _errorMessage = 'Нууц үг хамгийн багадаа 8 тэмдэгт байна');
      return;
    }
    if (password != secondPassword) {
      setState(() => _errorMessage = 'Нууц үг таарахгүй байна');
      return;
    }

    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      await _auth.register(
        fullName: fullName,
        email: email,
        password: password,
      );
      if (!mounted) return;
      Navigator.pushNamedAndRemoveUntil(context, '/homepage', (route) => false);
    } on FirebaseAuthException catch (e) {
      setState(() => _errorMessage = AuthService.errorMessage(e));
    } catch (e) {
      setState(() => _errorMessage = '$e');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.sizeOf(context);

    return Scaffold(
      backgroundColor: Colors.white,
      body: Stack(
        children: [
          Positioned(
            top: -40,
            left: 0,
            right: 0,
            child: SvgPicture.asset(
              'assets/image/header.svg',
              width: size.width,
              height: 320,
            ),
          ),
          Positioned(
            top: 0,
            left: 0,
            child: Image.asset(
              'assets/image/under_header.png',
              width: 150,
              height: 150,
            ),
          ),
          SafeArea(
            child: SingleChildScrollView(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  const SizedBox(height: 80),
                  const Text(
                    'Тавтай морилно уу?',
                    style: TextStyle(
                      fontSize: 26,
                      fontWeight: FontWeight.bold,
                      color: Colors.white,
                    ),
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    'Орлого, зарлагаа хянахад тань тусална',
                    style: TextStyle(color: Colors.grey),
                  ),
                  const SizedBox(height: 150),
                  AuthTextField(
                    controller: _fullName,
                    hint: 'Бүтэн нэрээ оруулна уу',
                  ),
                  const SizedBox(height: 16),
                  AuthTextField(
                    controller: _email,
                    hint: 'Имэйлээ оруулна уу',
                    keyboardType: TextInputType.emailAddress,
                  ),
                  const SizedBox(height: 16),
                  PasswordField(
                    controller: _password,
                    hint: 'Нууц үгээ оруулаарай',
                  ),
                  const SizedBox(height: 16),
                  PasswordField(
                    controller: _secPassword,
                    hint: 'Нууц үгээ давтан оруулаарай',
                  ),
                  const SizedBox(height: 8),
                  if (_errorMessage != null)
                    Text(
                      _errorMessage!,
                      style: const TextStyle(color: Colors.red, fontSize: 15),
                    ),
                  const SizedBox(height: 24),
                  GradientButton(
                    text: 'Бүртгүүлэх',
                    isLoading: _isLoading,
                    onTap: _register,
                  ),
                  const SizedBox(height: 16),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      const Text('Хэрэглэгчийн эрх байгаа юу? '),
                      TextButton(
                        onPressed: () => Navigator.pushNamedAndRemoveUntil(
                          context,
                          '/login',
                          (route) => false,
                        ),
                        child: const Text(
                          'Нэвтрэх',
                          style: TextStyle(
                            color: Color(0xFF1A9B7A),
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 30),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
