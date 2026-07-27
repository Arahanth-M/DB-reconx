package com.dbtraining.reconx.dto;

import com.dbtraining.reconx.repository.entity.Trade;
import org.springframework.stereotype.Component;

@Component
public class TradeMapper {

    public TradeResponse toResponse(Trade trade) {
        if (trade == null) return null;
        
        Long instrumentId = trade.getInstrument() != null ? trade.getInstrument().getId() : null;
        String instrumentSymbol = trade.getInstrument() != null ? trade.getInstrument().getSymbol() : null;
        Long counterpartyId = trade.getCounterparty() != null ? trade.getCounterparty().getId() : null;
        String counterpartyName = trade.getCounterparty() != null ? trade.getCounterparty().getName() : null;
        
        return new TradeResponse(
            trade.getId(),
            trade.getTradeRef(),
            instrumentId,
            instrumentSymbol,
            counterpartyId,
            counterpartyName,
            trade.getAssetClass(),
            trade.getSide(),
            trade.getQuantity(),
            trade.getPrice(),
            trade.getTradeDate(),
            trade.getStatus(),
            trade.getCreatedAt(),
            trade.getModifiedAt()
        );
    }
}
