import 'package:cloud_firestore/cloud_firestore.dart';
import '../models/pending_payment.dart';
import '../models/transaction_item.dart';

class PendingPaymentService {
  final String uid;

  PendingPaymentService(this.uid);

  DocumentReference<Map<String, dynamic>> get _userDoc =>
      FirebaseFirestore.instance.collection('users').doc(uid);

  CollectionReference<Map<String, dynamic>> get _pending =>
      _userDoc.collection('pending_payments');

  CollectionReference<Map<String, dynamic>> get _transactions =>
      _userDoc.collection('transactions');

  // Төлөх огноогоор нь эрэмбэлж бодит цагт өгнө
  Stream<List<PendingPayment>> watchPending() {
    return _pending
        .orderBy('dueDate')
        .snapshots()
        .map((snap) => snap.docs.map(PendingPayment.fromDoc).toList());
  }

  Future<void> addPending(PendingPayment p) => _pending.add(p.toMap());

  // Төлөх: зарлагын гүйлгээ нэмээд, хүлээгдэж буйгаас устгана (нэг transaction)
  Future<void> pay(PendingPayment p) {
    return FirebaseFirestore.instance.runTransaction((tx) async {
      final ref = _pending.doc(p.id);
      final snap = await tx.get(ref);
      if (!snap.exists) return; // аль хэдийн төлөгдсөн бол юу ч хийхгүй

      final expense = TransactionItem(
        id: '',
        title: p.title,
        date: DateTime.now(),
        amount: p.amount,
        isIncome: false,
      );
      tx.set(_transactions.doc(), expense.toMap());
      tx.delete(ref);
    });
  }
}
