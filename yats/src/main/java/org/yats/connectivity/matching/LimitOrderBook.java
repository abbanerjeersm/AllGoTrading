package org.yats.connectivity.matching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yats.common.CommonExceptions;
import org.yats.common.Decimal;
import org.yats.common.Tool;
import org.yats.common.UniqueId;
import org.yats.trading.*;

public class LimitOrderBook implements IConsumeReceipt {

    final Logger log = LoggerFactory.getLogger(LimitOrderBook.class);

    public int getOrderCount() {
        return book[0].getOrderCount() + book[1].getOrderCount();
    }

    public void match(OrderNew orderNew) {
        Receipt takerReceipt = orderNew.createReceiptDefault();
        int oppositeIndex = takerReceipt.getBookSide().toOpposite().toIndex();
        book[oppositeIndex].match(takerReceipt);
        int index = takerReceipt.getBookSide().toIndex();
        if(!takerReceipt.isEndState()) book[index].add(takerReceipt);
        sendPriceData();
    }

    public void cancel(UniqueId orderId) {
        book[0].cancel(orderId);
        book[1].cancel(orderId);
        sendPriceData();
    }

    public boolean isOrderInBooks(UniqueId orderId){
        boolean inBookBid =book[0].isOrderIdInBook(orderId.toString());
        boolean inBookAsk =book[1].isOrderIdInBook(orderId.toString());
        return inBookBid || inBookAsk;
    }

    @Override
    public void onReceipt(Receipt receipt) {
        lastReceipt=receipt;
        consumer.onReceipt(receipt);
    }

    //todo: only send if changed
    private void sendPriceData() {
        try {
            Decimal bid = book[0].getFrontRowPrice();
            Decimal bidSize = book[0].getFrontRowSize();
            Decimal ask = book[1].getFrontRowPrice();
            Decimal askSize = book[1].getFrontRowSize();

            Decimal last = lastReceipt != null ? lastReceipt.getPrice() : Decimal.ZERO;
            Decimal lastSize = lastReceipt != null ? lastReceipt.getCurrentTradedSize() : Decimal.ZERO;
            PriceData m = new PriceData(Tool.getUTCTimestamp(), productId, bid, ask, last, bidSize, askSize, lastSize);
            OfferBook offerBook = new OfferBook(
                    book[0].toOfferBookSide(10),
                    book[1].toOfferBookSide(10));
            m.setBook(offerBook);
            consumer.onPriceData(m);
        } catch(CommonExceptions.ContainerEmptyException e) {
            log.error(e.getMessage());
        } catch(Throwable t) {
            log.error(t.getMessage());
        }
    }

    public int getSize(BookSide _side) {
        return book[_side.toIndex()].getSize();
    }

    public int getSize() {
        return book[0].getSize() + book[1].getSize();
    }

    public LimitOrderBook(String _productId, IConsumePriceDataAndReceipt _consumer)
    {
        productId=_productId;
        consumer = _consumer;
        book = new LimitOrderBookSide[2];
        book[0] = new LimitOrderBookSide(BookSide.BID, this);
        book[1] = new LimitOrderBookSide(BookSide.ASK, this);
    }

    private Receipt lastReceipt;
    private LimitOrderBookSide book[];
    private IConsumePriceDataAndReceipt consumer;
    private String productId;

} // class
