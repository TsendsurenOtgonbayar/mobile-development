import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

import '../constants/app_colors.dart';
import '../models/transaction_item.dart';
import '../services/transaction_services.dart';
import '../utils/formatters.dart';
import '../services/auth_service.dart';
// ======================= HomePage =======================

class HomePage extends StatefulWidget {
  final bool hasUnreadNotifications;
  final VoidCallback? onNotificationTap;
  const HomePage({
    super.key,
    this.hasUnreadNotifications = true,
    this.onNotificationTap,
  });
  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  late final _user = AuthService().currentUser!;
  late final _service = TransactionService(_user.uid);
  late final Stream<List<TransactionItem>> _stream =_service.watchTransactions();
  @override
  Widget build(BuildContext context) {
    final topPadding = MediaQuery.of(context).padding.top;

    return Scaffold(
      backgroundColor: Colors.white,
      body: Stack(
        children: [
          // Дээд SVG дэвсгэр
          Positioned(
            top: 0,
            left: 0,
            right: 0,
            height: 320 + topPadding - 60,
            child: SvgPicture.asset(
              'assets/image/header.svg',
              fit: BoxFit.cover,
              alignment: Alignment.topCenter,
            ),
          ),

          Positioned.fill(
            child: SafeArea(
              bottom: false,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(20, 16, 20, 0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    HomeHeader(
                      userName: _user.displayName ?? 'Хэрэглэгч',
                      hasUnreadNotifications: widget.hasUnreadNotifications,
                      onNotificationTap: widget.onNotificationTap,
                    ),
                    const SizedBox(height: 36),
                    Expanded(
                      child: StreamBuilder<List<TransactionItem>>(
                        stream: _stream,
                        builder: (context, snapshot) {
                          if (snapshot.hasError) {
                            return Center(
                              child: Text('Алдаа гарлаа: ${snapshot.error}'),
                            );
                          }
                          if (!snapshot.hasData) {
                            return const Center(
                              child: CircularProgressIndicator(),
                            );
                          }

                          final items = snapshot.data!;
                          final income = items
                              .where((e) => e.isIncome)
                              .fold<double>(0, (s, e) => s + e.amount);
                          final expense = items
                              .where((e) => !e.isIncome)
                              .fold<double>(0, (s, e) => s + e.amount);

                          return Column(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: [
                              BalanceCard(
                                balance: income - expense,
                                income: income,
                                expense: expense,
                              ),
                              const SizedBox(height: 24),
                              const SectionHeader(title: 'Гүйлгээний түүх'),
                              const SizedBox(height: 8),
                              Expanded(
                                child: items.isEmpty
                                    ? const Center(
                                        child: Text('Гүйлгээ байхгүй байна'),
                                      )
                                    : ListView.builder(
                                        padding: const EdgeInsets.only(bottom: 24),
                                        itemCount: items.length,
                                        itemBuilder: (context, index) =>TransactionTile(item: items[index]),
                                      ),
                              ),
                            ],
                          );
                        },
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

// ======================= HomeHeader =======================

class HomeHeader extends StatelessWidget {
  final String userName;
  final bool hasUnreadNotifications;
  final VoidCallback? onNotificationTap;

  const HomeHeader({
    super.key,
    required this.userName,
    this.hasUnreadNotifications = false,
    this.onNotificationTap,
  });

  String _greetingByTime() {
    final hour = DateTime.now().hour;
    if (hour < 12) return 'Өглөөний мэнд?';
    if (hour < 18) return 'Өдрийн мэнд?';
    return 'Оройн мэнд?';
  }

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              _greetingByTime(),
              style: TextStyle(
                color: Colors.white.withOpacity(0.85),
                fontSize: 14,
                fontWeight: FontWeight.w400,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              userName,
              style: const TextStyle(
                color: Colors.white,
                fontSize: 20,
                fontWeight: FontWeight.w700,
              ),
            ),
          ],
        ),
        Material(
          color: Colors.white.withOpacity(0.14),
          borderRadius: BorderRadius.circular(12),
          child: InkWell(
            borderRadius: BorderRadius.circular(12),
            onTap: onNotificationTap,
            child: SizedBox(
              width: 44,
              height: 44,
              child: Center(
                child: Stack(
                  clipBehavior: Clip.none,
                  children: [
                    const Icon(
                      Icons.notifications_outlined,
                      color: Colors.white,
                      size: 24,
                    ),
                    if (hasUnreadNotifications)
                      Positioned(
                        top: 1,
                        right: 1,
                        child: Container(
                          width: 8,
                          height: 8,
                          decoration: const BoxDecoration(
                            color: AppColors.accent,
                            shape: BoxShape.circle,
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

// ======================= BalanceCard =======================

class BalanceCard extends StatelessWidget {
  final double balance;
  final double income;
  final double expense;
  final VoidCallback? onMoreTap;

  const BalanceCard({
    super.key,
    required this.balance,
    required this.income,
    required this.expense,
    this.onMoreTap,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        color: AppColors.primary,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: AppColors.primaryDark.withOpacity(0.25),
            blurRadius: 16,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Нийт үлдэгдэл',
                      style: TextStyle(fontSize: 14, color: Colors.white70),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      '${formatMoney(balance)}₮',
                      style: const TextStyle(
                        fontSize: 28,
                        color: Colors.white,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
                IconButton(
                  icon: const Icon(Icons.more_horiz),
                  color: Colors.white,
                  onPressed: onMoreTap ?? () {},
                ),
              ],
            ),
            const SizedBox(height: 24),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                _SummaryItem(
                  icon: Icons.arrow_downward,
                  title: 'Орлого',
                  amount: '${formatMoney(income)}₮',
                ),
                _SummaryItem(
                  icon: Icons.arrow_upward,
                  title: 'Зарлага',
                  amount: '${formatMoney(expense)}₮',
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _SummaryItem extends StatelessWidget {
  final IconData icon;
  final String title;
  final String amount;

  const _SummaryItem({
    required this.icon,
    required this.title,
    required this.amount,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Icon(icon, color: Colors.white70, size: 18),
            const SizedBox(width: 6),
            Text(
              title,
              style: const TextStyle(fontSize: 14, color: Colors.white70),
            ),
          ],
        ),
        const SizedBox(height: 6),
        Text(
          amount,
          style: const TextStyle(
            fontWeight: FontWeight.w600,
            fontSize: 18,
            color: Colors.white,
          ),
        ),
      ],
    );
  }
}

// ======================= SectionHeader =======================

class SectionHeader extends StatelessWidget {
  final String title;
  final String actionText;
  final VoidCallback? onActionTap;

  const SectionHeader({
    super.key,
    required this.title,
    this.actionText = 'Бүгдийг харах',
    this.onActionTap,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          title,
          style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
        ),
        TextButton(
          onPressed: onActionTap ?? () {},
          child: Text(actionText),
        ),
      ],
    );
  }
}

// ======================= TransactionTile =======================

class TransactionTile extends StatelessWidget {
  final TransactionItem item;

  const TransactionTile({super.key, required this.item});

  @override
  Widget build(BuildContext context) {
    final color = item.isIncome ? Colors.green : Colors.red;
    final sign = item.isIncome ? '+' : '-';

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Row(
        children: [
          Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(
              color: color.withOpacity(0.12),
              shape: BoxShape.circle,
            ),
            child: Icon(
              item.isIncome ? Icons.arrow_downward : Icons.arrow_upward,
              color: color,
              size: 20,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  item.title,
                  style: const TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  formatDate(item.date),
                  style: const TextStyle(fontSize: 12, color: Colors.grey),
                ),
              ],
            ),
          ),
          Text(
            '$sign${formatMoney(item.amount)}₮',
            style: TextStyle(
              fontSize: 15,
              fontWeight: FontWeight.w700,
              color: color,
            ),
          ),
        ],
      ),
    );
  }
}
