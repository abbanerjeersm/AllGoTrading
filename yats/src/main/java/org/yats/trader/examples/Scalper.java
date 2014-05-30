package org.yats.trader.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yats.common.Decimal;
import org.yats.trader.StrategyBase;
import org.yats.trading.*;

public class Scalper extends StrategyBase {

    // the configuration file log4j.properties for Log4J has to be provided in the working directory
    // an example of such a file is at config/log4j.properties.
    // if Log4J gives error message that it need to be configured, copy this file to the working directory
    final Logger log = LoggerFactory.getLogger(QuotingStrategy.class);

    @Override
    public void onMarketData(MarketData marketData)
    {
        if(!marketData.hasProductId(tradeProductId)) return;
        if(!startPrice.isSameAs(MarketData.NULL)) return;
        if(shuttingDown) return;
        startPrice = marketData;
        sendQuote(BookSide.BID, startPrice.getBid());
        sendQuote(BookSide.ASK, startPrice.getAsk());
    }

    private void sendQuote(BookSide side, Decimal startPrice) {

    }

    @Override
    public void onReceipt(Receipt receipt)
    {
        if(shuttingDown) return;
        if(receipt.getRejectReason().length()>0) {
            log.error("Received rejection! Stopping for now!");
            System.exit(-1);
        }
        if(!receipt.hasProductId(tradeProductId)){
            log.error("Received receipt for unknown product: " + receipt);
            return;
        }
        if(receipt.isForOrder(lastBidOrder)) {
            receivedOrderReceiptBidSide =true;
        }
        log.debug("Received receipt: " + receipt);
        position = receipt.getPositionChange().add(position);
    }

    @Override
    public void init()
    {
        setExternalAccount(getConfig("externalAccount"));
        setInternalAccount("quoting1");
        tradeProductId = getConfig("tradeProductId");
        subscribe(tradeProductId);
    }

    @Override
    public void shutdown()
    {
        shuttingDown=true;
        if(isInMarketBidSide() && receivedOrderReceiptBidSide) cancelLastOrderBidSide();
    }

//    private void handleMarketDataBidSide(MarketData marketData) {
//
//        if(isInMarketBidSide()) {
//            boolean changedSinceLastTick = !marketData.isPriceAndSizeSame(previousMarketData);
//            Decimal lastBid = lastBidOrder.getLimit();
//            Decimal bid = marketData.getBid();
//            Decimal bidChange=lastBid.subtract(getNewBid(bid)).abs();
//            if(changedSinceLastTick && bidChange.isGreaterThan(Decimal.fromDouble(0.01))) {
//                log.info("changed price since last order: " + marketData);
//            }
//
//            boolean bidChangedEnoughForOrderUpdate = bidChange.isGreaterThan(Decimal.fromDouble(0.02));
//            if(!bidChangedEnoughForOrderUpdate) return;
//
//            if(isInMarketBidSide() && receivedOrderReceiptBidSide) cancelLastOrderBidSide();
//        }
//
//        boolean positionLessThanMaximum = position.isLessThan(Decimal.ONE);
//        if(!isInMarketBidSide() && positionLessThanMaximum) {
//            Decimal bid = marketData.getBid();
//            sendOrderBidSide(getNewBid(bid));
//        }
//        previousMarketData=marketData;
//    }

    private Decimal getNewBid(Decimal oldBid) {
        Decimal bidRelative = oldBid.multiply(Decimal.fromDouble(0.995));
        Decimal bidAbsolute = oldBid.subtract(Decimal.fromDouble(0.05));
        return Decimal.min(bidRelative, bidAbsolute);
    }

    private void sendOrderBidSide(Decimal bid)
    {
        lastBidOrder = OrderNew.create()
                .withProductId(tradeProductId)
                .withExternalAccount(getExternalAccount())
                .withInternalAccount(getInternalAccount())
                .withBookSide(BookSide.BID)
                .withLimit(bid)
                .withSize(Decimal.fromDouble(0.01));
        receivedOrderReceiptBidSide = false;
        sendNewOrder(lastBidOrder);
    }

    private void cancelLastOrderBidSide() {
        OrderCancel o = lastBidOrder.createCancelOrder();
        sendOrderCancel(o);
        lastBidOrder=OrderNew.NULL;
        receivedOrderReceiptBidSide = false;
    }

    private boolean isInMarketBidSide() {
        return lastBidOrder != OrderNew.NULL;
    }


    public Scalper() {
        super();
        startPrice = MarketData.NULL;

        lastBidOrder = OrderNew.NULL;
        shuttingDown=false;
        previousMarketData = MarketData.NULL;
        position = Decimal.ZERO;
    }

    private MarketData startPrice;


    private Decimal position;
    private boolean shuttingDown;
    private String tradeProductId;
    private OrderNew lastBidOrder;

    private MarketData previousMarketData;
    private boolean receivedOrderReceiptBidSide;

} // class
