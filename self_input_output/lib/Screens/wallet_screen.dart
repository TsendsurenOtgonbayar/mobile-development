import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

import '../constants/app_colors.dart';
import '../models/pending_payment.dart';
import '../models/transaction_item.dart';
import '../services/auth_service.dart';
import '../services/transaction_services.dart'; 
import '../utils/formatters.dart';
import '../services/pending_payment_service.dart';
const Color _green = Color(0xFF3DB27A);
const Color _red = Color(0xFFFF6B5E);
const Color _tileBg = Color(0xFFF1F6F6);

class WalletScreen extends StatefulWidget {
  final bool hasUnreadNotifications;
  final VoidCallback? onNotificationTap;
  final VoidCallback? onBack;

  const WalletScreen({
    super.key,
    this.hasUnreadNotifications = true,
    this.onNotificationTap,
    this.onBack
  });

  @override
  State<WalletScreen> createState() => _WalletScreenState();
}

class _WalletScreenState extends State<WalletScreen> {
  late final _user = AuthService().currentUser!;
  late final Stream<List<TransactionItem>> _stream =TransactionService(_user.uid).watchTransactions();
  int _tabIndex = 0;
  late final _pendingService = PendingPaymentService(_user.uid);
  late final Stream<List<PendingPayment>> _pendingStream =_pendingService.watchPending();
  final Set<String> _payingIds = {};
 
 Future<void> _pay(PendingPayment p) async {
  if (_payingIds.contains(p.id)) return;
  setState(() => _payingIds.add(p.id));

  try {
    await _pendingService.pay(p);
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('${p.title} төлөгдлөө')),
    );
  } catch (e) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Төлөхөд алдаа гарлаа: $e')),
    );
  } finally {
    if (mounted) setState(() => _payingIds.remove(p.id));
  }
}
 
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
            height: topPadding + 220,
            child: SvgPicture.asset(
              'assets/image/header.svg',
              fit: BoxFit.cover,
              alignment: Alignment.topCenter,
            ),
          ),
          SafeArea(
            bottom: false,
            child: Column(
              children: [
                const SizedBox(height: 30),
                _WalletTopBar(
                  hasUnreadNotifications: widget.hasUnreadNotifications,
                  onNotificationTap: widget.onNotificationTap,
                  onBack: widget.onBack ??
                  ( Navigator.canPop(context)? () => Navigator.pop(context): null),
                ),
                const SizedBox(height: 40),
                Expanded(child: _buildSheet()),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSheet() {
    return Container(
      width: double.infinity,
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(30)),
      ),
      child: StreamBuilder<List<TransactionItem>>(
        stream: _stream,
        builder: (context, snapshot) {
          if (snapshot.hasError) {
            return Center(child: Text('Алдаа гарлаа: ${snapshot.error}'));
          }
          if (!snapshot.hasData) {
            return const Center(child: CircularProgressIndicator());
          }

          final items = snapshot.data!;
          final balance = items.fold<double>(
            0,
            (sum, e) => sum + (e.isIncome ? e.amount : -e.amount),
          );

          return Column(
            children: [
              const SizedBox(height: 28),
              const Text(
                'Нийт үлдэгдэл',
                style: TextStyle(fontSize: 16, color: Colors.grey),
              ),
              const SizedBox(height: 8),
              Text(
                '\$${formatMoney(balance)}',
                style: const TextStyle(
                  fontSize: 32,
                  fontWeight: FontWeight.w800,
                  color: Color(0xFF2B2B2B),
                ),
              ),
              const SizedBox(height: 28),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceAround,
                children: [
                  _ActionButton(
                    icon: Icons.add,
                    label: 'Нэмэх',
                    onTap: () {
                      // Гүйлгээ нэмэх хуудас руу
                    },
                  ),
                  _ActionButton(
                    icon: Icons.qr_code_scanner,
                    label: 'Төлбөр',
                    onTap: () {
                      // Төлбөр хийх хуудас руу
                    },
                  ),
                ],
              ),
              const SizedBox(height: 28),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: _SegmentedTabs(
                  labels: const ['Гүйлгээнүүд', 'Хүлээгдэж буй гүйлгээ'],
                  selectedIndex: _tabIndex,
                  onChanged: (i) => setState(() => _tabIndex = i),
                ),
              ),
              const SizedBox(height: 12),
              Expanded(
                child: _tabIndex == 0
                    ? _buildTransactionList(items)
                    : _buildPendingList(),
              ),
            ],
          );
        },
      ),
    );
  }

  Widget _buildTransactionList(List<TransactionItem> items) {
    if (items.isEmpty) {
      return const Center(child: Text('Гүйлгээ байхгүй байна'));
    }
    return ListView.builder(
      key: const ValueKey('transactions'),
      padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
      itemCount: items.length,
      itemBuilder: (context, i) => _TransactionRow(item: items[i]),
    );
  }

  Widget _buildPendingList() {
  return StreamBuilder<List<PendingPayment>>(
    stream: _pendingStream,
    builder: (context, snapshot) {
      if (snapshot.hasError) {
        return Center(child: Text('Алдаа гарлаа: ${snapshot.error}'));
      }
      if (!snapshot.hasData) {
        return const Center(child: CircularProgressIndicator());
      }

      final pending = snapshot.data!;
      if (pending.isEmpty) {
        return const Center(child: Text('Хүлээгдэж буй гүйлгээ байхгүй'));
      }

      return ListView.builder(
        key: const ValueKey('pending'),
        padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
        itemCount: pending.length,
        itemBuilder: (context, i) {
          final p = pending[i];
          return _PendingRow(
            payment: p,
            // Төлж байх үед товч идэвхгүй болно
            onPay: _payingIds.contains(p.id) ? null : () => _pay(p),
          );
        },
      );
    },
  );
}
}

// ======================= Top bar =======================

class _WalletTopBar extends StatelessWidget {
  final bool hasUnreadNotifications;
  final VoidCallback? onNotificationTap;
  final VoidCallback? onBack;

  const _WalletTopBar({
    required this.hasUnreadNotifications,
    this.onNotificationTap,
    this.onBack,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 56,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            SizedBox(
              width: 44,
              child: onBack == null
                  ? null
                  : IconButton(
                      padding: EdgeInsets.zero,
                      alignment: Alignment.centerLeft,
                      icon: const Icon(Icons.arrow_back_ios_new,
                          color: Colors.white, size: 20),
                      onPressed: onBack,
                    ),
            ),
            const Text(
              'Түрийвч',
              style: TextStyle(
                color: Colors.white,
                fontSize: 18,
                fontWeight: FontWeight.w700,
              ),
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
                        const Icon(Icons.notifications_outlined,
                            color: Colors.white, size: 24),
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
        ),
      ),
    );
  }
}

// ======================= Нэмэх / Төлбөр товч =======================

class _ActionButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;

  const _ActionButton({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: Column(
        children: [
          Container(
            width: 56,
            height: 56,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              border: Border.all(color: AppColors.primary, width: 1),
            ),
            child: Icon(icon, color: AppColors.primary, size: 26),
          ),
          const SizedBox(height: 8),
          Text(
            label,
            style: const TextStyle(fontSize: 13, color: Color(0xFF444444)),
          ),
        ],
      ),
    );
  }
}

// ======================= Segmented tab =======================

class _SegmentedTabs extends StatelessWidget {
  final List<String> labels;
  final int selectedIndex;
  final ValueChanged<int> onChanged;

  const _SegmentedTabs({
    required this.labels,
    required this.selectedIndex,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: const Color(0xFFF3F5F5),
        borderRadius: BorderRadius.circular(30),
      ),
      child: Row(
        children: List.generate(labels.length, (i) {
          final selected = i == selectedIndex;
          return Expanded(
            child: GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: () => onChanged(i),
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 200),
                height: 40,
                alignment: Alignment.center,
                padding: const EdgeInsets.symmetric(horizontal: 8),
                decoration: BoxDecoration(
                  color: selected ? Colors.white : Colors.transparent,
                  borderRadius: BorderRadius.circular(26),
                  boxShadow: selected
                      ? [
                          BoxShadow(
                            color: Colors.black.withOpacity(0.06),
                            blurRadius: 8,
                            offset: const Offset(0, 2),
                          ),
                        ]
                      : null,
                ),
                child: Text(
                  labels[i],
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: selected
                        ? const Color(0xFF2B2B2B)
                        : Colors.grey.shade600,
                  ),
                ),
              ),
            ),
          );
        }),
      ),
    );
  }
}

// ======================= Мөрүүд =======================

class _TileIcon extends StatelessWidget {
  final IconData icon;

  const _TileIcon({required this.icon});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 44,
      height: 44,
      decoration: BoxDecoration(
        color: _tileBg,
        borderRadius: BorderRadius.circular(10),
      ),
      child: Icon(icon, color: AppColors.primary, size: 22),
    );
  }
}

class _TransactionRow extends StatelessWidget {
  final TransactionItem item;

  const _TransactionRow({required this.item});

  @override
  Widget build(BuildContext context) {
    final color = item.isIncome ? _green : _red;
    final sign = item.isIncome ? '+' : '-';

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          _TileIcon(
            icon: item.isIncome ? Icons.arrow_downward : Icons.arrow_upward,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  item.title,
                  style: const TextStyle(
                      fontSize: 15, fontWeight: FontWeight.w500),
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
            '$sign \$${formatMoney(item.amount)}',
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

class _PendingRow extends StatelessWidget {
  final PendingPayment payment;
  final VoidCallback? onPay;

  const _PendingRow({required this.payment, required this.onPay});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          _TileIcon(icon: payment.icon),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  payment.title,
                  style: const TextStyle(
                      fontSize: 15, fontWeight: FontWeight.w500),
                ),
                const SizedBox(height: 2),
                Text(
                  formatDate(payment.dueDate),
                  style: const TextStyle(fontSize: 12, color: Colors.grey),
                ),
              ],
            ),
          ),
          Material(
            color: const Color(0xFFE8F4F3),
            borderRadius: BorderRadius.circular(20),
            child: InkWell(
              borderRadius: BorderRadius.circular(20),
              onTap: onPay,
              child: const Padding(
                padding: EdgeInsets.symmetric(horizontal: 22, vertical: 9),
                child: Text(
                  'Төлөх',
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: AppColors.primary,
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
