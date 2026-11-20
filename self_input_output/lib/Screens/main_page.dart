import 'package:flutter/material.dart';
import 'homepage.dart';
import 'profile_screen.dart';
class MainPage extends StatefulWidget {
  const MainPage({super.key});

  @override
  State<MainPage> createState() => _MainPageState();
}

class _MainPageState extends State<MainPage> {
  int _currentIndex = 0;

  // Tab бүрийн хуудас (одоохондоо түр Center-ээр)
  final List<Widget> _pages = const [
    HomePage(),
    Center(child: Text('Статистик')),
    Center(child: Text('Түрийвч')),
    ProfileScreen(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      // IndexedStack нь tab солиход хуудасны төлөв (scroll г.м.) хадгална
      body: IndexedStack(
        index: _currentIndex,
        children: _pages,
      ),
      bottomNavigationBar: NavigationBar(
        height:60,
        labelBehavior: NavigationDestinationLabelBehavior.alwaysHide,
        selectedIndex: _currentIndex,
        onDestinationSelected: (index) {
          setState(() => _currentIndex = index);
        },
        backgroundColor: Colors.white,
        indicatorColor: const Color(0xFF438883).withOpacity(0.15),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.home_outlined,size: 30),
            selectedIcon: Icon(Icons.home,size:30, color: Color(0xFF438883)),
            label: 'nuur',
          ),
          NavigationDestination(
            icon: Icon(Icons.bar_chart_outlined,size: 30),
            selectedIcon: Icon(Icons.bar_chart,size:30, color: Color(0xFF438883)),
            label: 'statistic',
          ),
          NavigationDestination(
            icon: Icon(Icons.account_balance_wallet_outlined,size:30),
            selectedIcon:
                Icon(Icons.account_balance_wallet,size:30, color: Color(0xFF438883)),
            label: 'wallet',
          ),
          NavigationDestination(
            icon: Icon(Icons.person_outline,size:30),
            selectedIcon: Icon(Icons.person,size:30, color: Color(0xFF438883)),
            label: 'profile',
          ),
        ],
      ),
    );
  }
}
