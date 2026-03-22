package com.ces.erp.garage.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GarageNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyEquipmentChanged(String action, Long equipmentId) {
        messagingTemplate.convertAndSend("/topic/garage",
                Map.of("action", action, "equipmentId", equipmentId));
    }

    public void notifyBulkChange() {
        messagingTemplate.convertAndSend("/topic/garage",
                Map.of("action", "BULK_UPDATE"));
    }
}
