import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:qr_flutter/qr_flutter.dart';

import '../constants/app_colors.dart';
import '../models/pending_payment.dart';
import '../models/transaction_item.dart';
import '../services/auth_service.dart';
import '../services/pending_payment_service.dart';
import '../services/transaction_services.dart';
import '../services/wallet_service.dart';
import '../utils/formatters.dart';

const Color _green = Color(0xFF3DB27A);
const Color _red = Color(0xFFFF6B5E);
const Color _tileBg = Color(0xFFF1F6F6);

class WalletScreen extends StatefulWidget {
  final bool hasUnreadNotifications;
  final VoidCallback? onNotificationTap;
  final VoidCallback? onBack;

  const WalletScreen({
    super.key,
    this.hasUnreadNotifications = false,
    this.onNotificationTap,
    this.onBack,
  });

  @override
  State<WalletScreen> createState() => _WalletScreenState();
}

class _WalletScreenState extends State<WalletScreen> {
  late final _user = AuthService().currentUser!;
  late final Stream<List<TransactionItem>> _stream =
      TransactionService(_user.uid).watchTransactions();
  late final _pendingService = PendingPaymentService(_user.uid);
  late final Stream<List<PendingPayment>> _pendingStream =
      _pendingService.watchPending();
  late final _wallet = WalletService(_user.uid);

  final Set<String> _payingIds = {};
  int _tabIndex = 0;
  int _sheetMode = 0;
  int _chargeTab = 0;

  String get _title {
    switch (_sheetMode) {
      case 1:
        return _chargeTab == 0 ? 'Түрийвч цэнэглэх' : 'Түрийвчтэй холбох';
      case 2:
        return 'Төлбөр нэмэх';
      default:
        return 'Түрийвч';
    }
  }

  // Нэг шат буцна: Аккаунт tab -> Картууд -> Wallet -> өмнөх хуудас
  VoidCallback? get _backAction {
    if (_sheetMode != 0) {
      return () => setState(() {
            if (_sheetMode == 1 && _chargeTab == 1) {
              _chargeTab = 0;
            } else {
              _sheetMode = 0;
            }
          });
    }
    return widget.onBack ??
        (Navigator.canPop(context) ? () => Navigator.pop(context) : null);
  }

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
                  title: _title,
                  hasUnreadNotifications: widget.hasUnreadNotifications,
                  onNotificationTap: widget.onNotificationTap,
                  onBack: _backAction,
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
    switch (_sheetMode) {
      case 1:
        return _ChargeView(
          wallet: _wallet,
          tab: _chargeTab,
          onTabChanged: (i) => setState(() => _chargeTab = i),
          onDone: () => setState(() {
            _sheetMode = 0;
            _chargeTab = 0;
          }),
        );
      case 2:
        return _PayView(
          wallet: _wallet,
          onDone: () => setState(() {
            _sheetMode = 0;
            _tabIndex = 1; 
          }),
        );
      case 3:
        return _BillDetails(// end 

        );
      case 4:
        return _BillPayment( // end

        );
      case 0:
      default:
        return _walletFirstTab();
    }
  }

  Widget _walletFirstTab() {
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
                    onTap: () => setState(() {
                      _sheetMode = 1;
                      _chargeTab = 0;
                    }),
                  ),
                  _ActionButton(
                    icon: Icons.qr_code_scanner,
                    label: 'Төлбөр',
                    onTap: () => setState(() => _sheetMode = 2),
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

//  Түрийвч цэнэглэх (Картууд / Аккаунт) 

class _ChargeView extends StatefulWidget {
  final WalletService wallet;
  final int tab;
  final ValueChanged<int> onTabChanged;
  final VoidCallback onDone;

  const _ChargeView({
    required this.wallet,
    required this.tab,
    required this.onTabChanged,
    required this.onDone,
  });

  @override
  State<_ChargeView> createState() => _ChargeViewState();
}

class _AccountOption {
  final String title;
  final String subtitle;
  final IconData icon;
  const _AccountOption(this.title, this.subtitle, this.icon);
}

class _ChargeViewState extends State<_ChargeView> {
  static const _accounts = [
    _AccountOption('Bank Link', 'Connect your bank\naccount to deposit & fund',
        Icons.account_balance),
    _AccountOption('Microdeposits', 'Connect bank in 5-7 days',
        Icons.monetization_on),
    _AccountOption('Paypal', 'Connect you paypal account', Icons.paypal),
  ];

  final _name = TextEditingController();
  final _number = TextEditingController();
  final _cvc = TextEditingController();
  final _expiry = TextEditingController();
  final _value = TextEditingController();

  int _account = 0;
  bool _busy = false;

  @override
  void dispose() {
    _name.dispose();
    _number.dispose();
    _cvc.dispose();
    _expiry.dispose();
    _value.dispose();
    super.dispose();
  }

  void _snack(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
  }

  Future<void> _sendCard() async {
    if (_name.text.trim().isEmpty || _number.text.trim().isEmpty || _number.text.trim().length!=16) {
      _snack('Картын нэр, дугаараа зөв бөглөнө үү');
      return;
    }
    if (_expiry.text.trim().length!=4) {
      _snack('4 тоо оруулна уу');
      return;
    }
    final int? YEAR= int.tryParse(_expiry.text.trim().substring(0,2));
    final int? MONTH= int.tryParse(_expiry.text.trim().substring(4-2));
    if (YEAR==null || MONTH==null) {
      _snack('хоосон талбар байж болохгүй');
      return;
    }
    if(MONTH<=1 || MONTH>=12){
      _snack('MM дээр боломжийн утга оруул');
      return;
    }
    final expYEAR= 2000 + YEAR;
    final now = DateTime.now();
    final currentYear = now.year;
    final currentMonth = now.month;

    if (currentYear>=expYEAR) {
      _snack('Хугацаа нь дууссан байна');
      return;
    }else if(currentYear==expYEAR && currentMonth>=MONTH){
      _snack('Хугацаа дууссан байна');
      return;
    }
    if (_cvc.text.trim().length!=3) {
      _snack('CVC 3 оронтой байх ёстой');
      return;
    }
    setState(() => _busy = true);
    try {
      await widget.wallet.addCard(
        holderName: _name.text.trim(),
        cardNumber: _number.text.trim(),
        cvc: _cvc.text.trim(),
        expiry: _expiry.text.trim(),
        value: _value.text.trim(),
      );
      if (!mounted) return;
      _snack('Карт хадгалагдлаа');
      widget.onDone();
    } catch (e) {
      if (!mounted) return;
      _snack('Хадгалахад алдаа гарлаа: $e');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _linkAccount() async {
    setState(() => _busy = true);
    try {
      await widget.wallet.linkAccount(_accounts[_account].title);
      if (!mounted) return;
      _snack('Холбогдлоо, +\$1000 орлого нэмэгдлээ');
      widget.onDone();
    } catch (e) {
      if (!mounted) return;
      _snack('Холбоход алдаа гарлаа: $e');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  InputDecoration _dec(String label) => InputDecoration(
        labelText: label,
        labelStyle: const TextStyle(fontSize: 12, color: Colors.grey),
        contentPadding:
            const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8),
          borderSide: BorderSide(color: Colors.grey.shade300),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8),
          borderSide: const BorderSide(color: AppColors.primary, width: 1.5),
        ),
      );

  @override
  Widget build(BuildContext context) {
    return _SheetShell(
      child: SingleChildScrollView(
        padding: EdgeInsets.fromLTRB(
            20, 24, 20, MediaQuery.of(context).viewInsets.bottom + 24),
        child: Column(
          children: [
            _SegmentedTabs(
              labels: const ['Картууд', 'Аккаунт'],
              selectedIndex: widget.tab,
              onChanged: widget.onTabChanged,
            ),
            const SizedBox(height: 20),
            if (widget.tab == 0) ..._cardTab() else ..._accountTab(),
          ],
        ),
      ),
    );
  }

  List<Widget> _cardTab() {
    return [
      AnimatedBuilder(
        animation: Listenable.merge([_name, _number, _expiry]),
        builder: (context, _) => _CardPreview(
          number: _number.text,
          name: _name.text,
          expiry: _expiry.text,
        ),
      ),
      const SizedBox(height: 16),
      const Align(
        alignment: Alignment.centerLeft,
        child: Text('Картны мэдээлэлээ нэмэх',
            style: TextStyle(fontSize: 14, fontWeight: FontWeight.w700)),
      ),
      const SizedBox(height: 14),
      TextField(controller: _name, decoration: _dec('КАРТ ДЭЭРХ НЭР')),
      const SizedBox(height: 12),
      Row(
        children: [
          Expanded(
            flex: 3,
            child: TextField(
              controller: _number,
              keyboardType: TextInputType.number,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              decoration: _dec('КАРТЫН ДУГААР'),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            flex: 2,
            child: TextField(
              controller: _cvc,
              keyboardType: TextInputType.number,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              decoration: _dec('CVC'),
            ),
          ),
        ],
      ),
      const SizedBox(height: 12),
      Row(
        children: [
          Expanded(
            flex: 3,
            child: TextField(
                controller: _expiry,
                decoration: _dec('YY/MM')),
          ),
          const SizedBox(width: 12),
          Expanded(
            flex: 2,
            child: TextField(
              controller: _value,
              keyboardType:
                  const TextInputType.numberWithOptions(decimal: true),
              decoration: _dec('Value \$'),
            ),
          ),
        ],
      ),
      const SizedBox(height: 24),
      _OutlineButton(
          label: 'ИЛГЭЭХ', busy: _busy, onTap: _busy ? null : _sendCard),
    ];
  }

  List<Widget> _accountTab() {
    return [
      for (var i = 0; i < _accounts.length; i++)
        Padding(
          padding: const EdgeInsets.only(bottom: 14),
          child: GestureDetector(
            onTap: () => setState(() => _account = i),
            child: Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: i == _account ? const Color(0xFFE3F0EE) : _tileBg,
                borderRadius: BorderRadius.circular(16),
              ),
              child: Row(
                children: [
                  Container(
                    width: 44,
                    height: 44,
                    decoration: const BoxDecoration(
                        color: Colors.white, shape: BoxShape.circle),
                    child: Icon(_accounts[i].icon,
                        color: AppColors.primary, size: 22),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(_accounts[i].title,
                            style: const TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.w700,
                                color: AppColors.primary)),
                        const SizedBox(height: 2),
                        Text(_accounts[i].subtitle,
                            style: const TextStyle(
                                fontSize: 11, color: Colors.grey)),
                      ],
                    ),
                  ),
                  if (i == _account)
                    const Icon(Icons.check_circle,
                        color: AppColors.primary, size: 24),
                ],
              ),
            ),
          ),
        ),
      const SizedBox(height: 24),
      _OutlineButton(
          label: 'ДАРААХ', busy: _busy, onTap: _busy ? null : _linkAccount),
    ];
  }
}

class _CardPreview extends StatelessWidget {
  final String number;
  final String name;
  final String expiry;

  const _CardPreview({
    required this.number,
    required this.name,
    required this.expiry,
  });

  @override
  Widget build(BuildContext context) {
    final digits = number.padRight(16, '•');
    final grouped = [
      for (var i = 0; i < 16; i += 4) digits.substring(i, i + 4)
    ].join('   ');

    return Container(
      width: double.infinity,
      height: 170,
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(18),
        gradient: const LinearGradient(
          colors: [Color(0xFF3F9D8F), Color(0xFF1BB5A9)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('Debit Card',
                  style: TextStyle(color: Colors.white70, fontSize: 11)),
              Text('Mongol empire',
                  style: TextStyle(
                      color: Colors.white, fontWeight: FontWeight.w700)),
            ],
          ),
          const Spacer(),
          Text(grouped,
              style: const TextStyle(
                  color: Colors.white,
                  fontSize: 16,
                  letterSpacing: 1.5,
                  fontWeight: FontWeight.w600)),
          const SizedBox(height: 10),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(name.isEmpty ? 'CARD HOLDER' : name.toUpperCase(),
                  style: const TextStyle(color: Colors.white, fontSize: 12)),
              Text(expiry.isEmpty ? 'YY/MM' : expiry,
                  style: const TextStyle(color: Colors.white, fontSize: 12)),
            ],
          ),
        ],
      ),
    );
  }
}

// Төлбөр нэмэх 

class _PayView extends StatefulWidget {
  final WalletService wallet;
  final VoidCallback onDone;

  const _PayView({required this.wallet, required this.onDone});

  @override
  State<_PayView> createState() => _PayViewState();
}

class _PayViewState extends State<_PayView> {
  late final Stream<List<ExpenseType>> _types =
      widget.wallet.watchExpenseTypes();
  final _amount = TextEditingController();
  ExpenseType? _selected;
  DateTime _date = DateTime.now();
  bool _busy = false;

  @override
  void initState() {
    super.initState();
      widget.wallet.seedExpenseTypesIfEmpty();
  }

  @override
  void dispose() {
    _amount.dispose();
    super.dispose();
  }

  void _snack(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
  }

  Future<void> _pickDate() async {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);

    final picked = await showDatePicker(
      context: context,
      initialDate: _date.isBefore(today) ? today : _date,
      firstDate: today, 
      lastDate: DateTime(2100),
    );
    if (picked != null && !picked.isBefore(today)) {
      setState(() => _date = picked);
    }
  }
  Future<void> _submit() async {
    final type = _selected;
    final amount = double.tryParse(_amount.text.trim().replaceAll(',', '.'));
    if (type == null) {
      _snack('Гүйлгээний нэрээ сонгоно уу');
      return;
    }
    if (amount == null || amount <= 0) {
      _snack('Үнийн дүнг зөв оруулна уу');
      return;
    }

    setState(() => _busy = true);
    try {
      await widget.wallet.addPending(PendingPayment(
        id: '',
        title: type.title,
        amount: amount,
        dueDate: _date,
        iconKey: type.iconKey,
      ));
      if (!mounted) return;
      _snack('${type.title} хүлээгдэж буй гүйлгээнд нэмэгдлээ');
      widget.onDone();
    } catch (e) {
      if (!mounted) return;
      _snack('Хадгалахад алдаа гарлаа: $e');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Widget _label(String text) => Padding(
        padding: const EdgeInsets.only(bottom: 8, top: 18),
        child: Text(text,
            style: const TextStyle(
                fontSize: 11, color: Colors.grey, letterSpacing: 0.5)),
      );

  @override
  Widget build(BuildContext context) {
    return _SheetShell(
      child: SingleChildScrollView(
        padding: EdgeInsets.fromLTRB(
            20, 10, 20, MediaQuery.of(context).viewInsets.bottom + 24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _label('ГҮЙЛГЭЭНИЙ НЭР'),
            StreamBuilder<List<ExpenseType>>(
              stream: _types,
              builder: (context, snapshot) {
                final types = snapshot.data ?? const <ExpenseType>[];
                return DropdownButtonFormField<String>(
                  value: _selected?.id,
                  hint: const Text('Сонгоно уу'),
                  decoration: InputDecoration(
                    contentPadding: const EdgeInsets.symmetric(
                        horizontal: 14, vertical: 10),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                      borderSide: BorderSide(color: Colors.grey.shade300),
                    ),
                  ),
                  items: [
                    for (final t in types)
                      DropdownMenuItem(
                        value: t.id,
                        child: Row(
                          children: [
                            Icon(t.icon, color: AppColors.primary, size: 20),
                            const SizedBox(width: 10),
                            Text(t.title),
                          ],
                        ),
                      ),
                  ],
                  onChanged: (id) => setState(
                      () => _selected = types.firstWhere((t) => t.id == id)),
                );
              },
            ),
            _label('ҮНИЙН ДҮН'),
            TextField(
              controller: _amount,
              keyboardType:
                  const TextInputType.numberWithOptions(decimal: true),
              decoration: InputDecoration(
                prefixText: '\$ ',
                contentPadding:
                    const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
                enabledBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(8),
                  borderSide: const BorderSide(color: AppColors.primary),
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(8),
                  borderSide:
                      const BorderSide(color: AppColors.primary, width: 1.5),
                ),
              ),
            ),
            _label('ОГНОО'),
            InkWell(
              onTap: _pickDate,
              borderRadius: BorderRadius.circular(8),
              child: Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.grey.shade300),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(formatDate(_date)),
                    const Icon(Icons.calendar_today,
                        size: 18, color: Colors.grey),
                  ],
                ),
              ),
            ),
            _label('ТӨЛБӨР'),
            InkWell(
              onTap: _busy ? null : _submit,
              borderRadius: BorderRadius.circular(8),
              child: Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(vertical: 18),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.grey.shade300),
                ),
                child: _busy
                    ? const Center(
                        child: SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(strokeWidth: 2)))
                    : const Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.add_circle,
                              size: 20, color: Color(0xFF555555)),
                          SizedBox(width: 8),
                          Text('Төлбөр нэмэх',
                              style: TextStyle(
                                  fontSize: 13, color: Color(0xFF555555))),
                        ],
                      ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _SheetShell extends StatelessWidget {
  final Widget child;
  const _SheetShell({required this.child});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(30)),
      ),
      child: child,
    );
  }
}

class _OutlineButton extends StatelessWidget {
  final String label;
  final bool busy;
  final VoidCallback? onTap;

  const _OutlineButton({
    required this.label,
    required this.busy,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: double.infinity,
      height: 52,
      child: OutlinedButton(
        onPressed: onTap,
        style: OutlinedButton.styleFrom(
          side: const BorderSide(color: AppColors.primary),
          shape: const StadiumBorder(),
        ),
        child: busy
            ? const SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(strokeWidth: 2))
            : Text(label,
                style: const TextStyle(
                    color: AppColors.primary,
                    fontWeight: FontWeight.w700,
                    letterSpacing: 1)),
      ),
    );
  }
}

// ======================= Top bar =======================

class _WalletTopBar extends StatelessWidget {
  final String title;
  final bool hasUnreadNotifications;
  final VoidCallback? onNotificationTap;
  final VoidCallback? onBack;

  const _WalletTopBar({
    required this.title,
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
            Text(
              title,
              style: const TextStyle(
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
