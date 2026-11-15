import 'package:flutter/material.dart';
import 'dart:async';
class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState()=> _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen>{
  @override
  void initState(){
      super.initState();
      Timer(const Duration(seconds:3),(){
          Navigator.pushReplacementNamed(context,'/onboarding');
        });
    }
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: DecoratedBox(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [
              Color(0xFF6BB5AC),
              Color(0xFF438883),
            ],
          ),
        ),
        child: SizedBox.expand(
          child: Center(
            child: Text(
              'Сайн уу?',
              style: TextStyle(
                color: Colors.white,
                fontSize: 24,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
