import 'package:flutter/material.dart';
import 'package:firebase_core/firebase_core.dart';
import 'firebase_options.dart';
import 'screens/splash_screen.dart';
import 'screens/onboarding_screen.dart';
import 'screens/login_screen.dart';
import 'screens/register_screen.dart';
import 'screens/homepage.dart';
void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  try{
  await Firebase.initializeApp(
    options: DefaultFirebaseOptions.currentPlatform,
  );
  }on FirebaseAuthException catch(e){
    print("firebase: $e.code");
  }catch(e){
    print("aldaa $e")
  }

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
        '/auth': (context)=> const AuthGate(),
        '/login': (context) => const LoginScreen(),
        '/register': (context) => const RegisterScreen(),
        '/homepage':(context)=> const HomePageScreen(),
      },
    );
  }
}
class AuthGate extends StatelessWidget {
  const AuthGate({super.key});
  @override
  Widget build(BuildContext context) {
    return StreamBuilder<User?>(
      stream: FirebaseAuth.instance.authStateChanges(),
      builder: (context, snapshot) {
        // 1. Анх нэвтрэлтийн төлөв шалгаж байх үед
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Scaffold(
            body: Center(child: CircularProgressIndicator()),
          );
        }
        // 2. Алдаа гарсан бол
        if (snapshot.hasError) {
          return const Scaffold(
            body: Center(child: Text('Нэвтрэлтийн төлөв шалгахад алдаа гарлаа.')),
          );
        }
        // 3. Хэрэглэгч нэвтэрсэн байвал HomeScreen
        if (snapshot.hasData && snapshot.data != null) {
          //return const HomeScreen();
        }
        // 4. Нэвтрээгүй бол LoginScreen
        return const OnboardingScreen();
      },
    );
  }
}
