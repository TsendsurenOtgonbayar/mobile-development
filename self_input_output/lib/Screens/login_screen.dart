import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

import '../services/auth_service.dart';
import 'auth_widgets.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _auth = AuthService();
  final _email = TextEditingController();
  final _password = TextEditingController();
  bool _isLoading = false;
  String? _errorMessage;

  @override
  void dispose() {
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final email = _email.text.trim();
    final password = _password.text.trim();

    if (email.isEmpty || password.isEmpty) {
      setState(() => _errorMessage = 'Имэйл болон нууц үгээ оруулна уу');
      return;
    }

    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      await _auth.signIn(email: email, password: password);
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
          Positioned(
            top: 150,
            right: 50,
            child: Image.asset(
              'assets/image/header_logo.png',
              width: 250,
              height: 250,
            ),
          ),
          SafeArea(
            child: SingleChildScrollView(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  const SizedBox(height: 90),
                  const Text(
                    'Тавтай морилно уу !',
                    style: TextStyle(
                      fontSize: 26,
                      fontWeight: FontWeight.bold,
                      color: Colors.white,
                    ),
                  ),
                  const SizedBox(height: 300),
                  AuthTextField(
                    controller: _email,
                    hint: 'Имэйлээ оруулна уу',
                    keyboardType: TextInputType.emailAddress,
                  ),
                  const SizedBox(height: 16),
                  PasswordField(
                    controller: _password,
                    hint: 'Нууц үгээ оруулна уу',
                  ),
                  const SizedBox(height: 8),
                  if (_errorMessage != null)
                    Text(
                      _errorMessage!,
                      style: const TextStyle(color: Colors.red, fontSize: 15),
                    ),
                  const SizedBox(height: 32),
                  GradientButton(
                    text: 'Нэвтрэх',
                    isLoading: _isLoading,
                    onTap: _submit,
                  ),
                  const SizedBox(height: 16),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      const Text('Бүртгэлгүй юу? '),
                      TextButton(
                        onPressed: () => Navigator.pushNamedAndRemoveUntil(
                          context,
                          '/register',
                          (route) => false,
                        ),
                        child: const Text(
                          'Бүртгүүлэх',
                          style: TextStyle(
                            color: Color(0xFF1A9B7A),
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 40),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
