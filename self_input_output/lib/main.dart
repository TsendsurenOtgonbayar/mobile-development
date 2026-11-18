import 'package:flutter/material.dart';
import 'package:firebase_core/firebase_core.dart';
import 'firebase_options.dart';
import 'screens/splash_screen.dart';
import 'screens/onboarding_screen.dart';
import 'screens/login_screen.dart';
import 'screens/register_screen.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  try{

  
  await Firebase.initializeApp(
    options: DefaultFirebaseOptions.currentPlatform,
  );
    print("amjilttai ${Firebase.app().options.projectId}");
  }catch(e){
    print("amjiltgui $e");
    }
  runApp(const MyApp());
}
class TemplateLoading extends StatelessWidget{
    const TemplateLoading({super.key});

    @override
    Widget build(BuildContext context){
        return MaterialApp(
        debugShowCheckedModeBanner:false,
        home: Scaffold(
        body: Center(
          child: CircularProgressIndicator(),
          ),
        ),
        );
      }
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
