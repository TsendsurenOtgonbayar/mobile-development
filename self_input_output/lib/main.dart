import 'package:flutter/material.dart';
import 'package:firebase_core/firebase_core.dart'
import 'firebase_options.dart'
import 'screens/splash_screen.dart';
import 'screens/onboarding_screen.dart';
import 'screens/login_screen.dart';
import 'screens/register_screen.dart';

void main() {
  WigetsFlutterBinding.ensureInitialize();
  await Firebase.initializeApp(
    options: DefaultFirebaseOptions.currentPlatform,
  );
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Wallet App',
      theme: ThemeData(
        primaryColor: const Color(0xFF1A9B7A),
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF1A9B7A)),
        useMaterial3: true,
      ),
      home: const SplashScreen(),
      routes: {
        '/onboarding': (context) => const OnboardingScreen(),
        '/login': (context) => const LoginScreen(),
        '/register': (context) => const RegisterScreen(),
        //'/homepage':(context)=> const HomePageScreen(),
      },
    );
  }
}
