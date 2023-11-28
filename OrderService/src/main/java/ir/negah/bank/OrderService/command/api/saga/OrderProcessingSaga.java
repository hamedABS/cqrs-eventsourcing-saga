package ir.negah.bank.OrderService.command.api.saga;

import ir.negah.bank.CommonService.commands.CancelOrderCommand;
import ir.negah.bank.CommonService.commands.CancelPaymentCommand;
import ir.negah.bank.CommonService.commands.CompleteOrderCommand;
import ir.negah.bank.CommonService.commands.ShipOrderCommand;
import ir.negah.bank.CommonService.commands.ValidatePaymentCommand;
import ir.negah.bank.CommonService.events.OrderCancelledEvent;
import ir.negah.bank.CommonService.events.OrderCompletedEvent;
import ir.negah.bank.CommonService.events.OrderShippedEvent;
import ir.negah.bank.CommonService.events.PaymentCancelledEvent;
import ir.negah.bank.CommonService.events.PaymentProcessedEvent;
import ir.negah.bank.CommonService.model.User;
import ir.negah.bank.CommonService.queries.GetUserPaymentDetailsQuery;
import ir.negah.bank.OrderService.command.api.events.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@Saga
@Slf4j
public class OrderProcessingSaga {

    @Autowired
    private transient CommandGateway commandGateway;

    @Autowired
    private transient QueryGateway queryGateway;


    public OrderProcessingSaga() {
    }

    @StartSaga
    @SagaEventHandler(associationProperty = "orderId")
    private void handle(OrderCreatedEvent event) {
        log.info("OrderCreatedEvent in Saga for Order Id : {}",
                event.getOrderId());

        GetUserPaymentDetailsQuery getUserPaymentDetailsQuery
                = new GetUserPaymentDetailsQuery(event.getUserId());

        User user = null;

        try {
            user = queryGateway.query(
                    getUserPaymentDetailsQuery,
                    ResponseTypes.instanceOf(User.class)
            ).join();
            ValidatePaymentCommand validatePaymentCommand
                    = ValidatePaymentCommand
                    .builder()
                    .cardDetails(user.getCardDetails())
                    .orderId(event.getOrderId())
                    .paymentId(UUID.randomUUID().toString())
                    .build();

            commandGateway.sendAndWait(validatePaymentCommand);


        } catch (Exception e) {
            log.error(e.getMessage());
            //Start the Compensating transaction
            cancelOrderCommand(event.getOrderId());
        }

    }

    private void cancelOrderCommand(String orderId) {
        CancelOrderCommand cancelOrderCommand
                = new CancelOrderCommand(orderId);
        commandGateway.send(cancelOrderCommand);
    }

    @SagaEventHandler(associationProperty = "orderId")
    private void handle(PaymentProcessedEvent event) {
        log.info("PaymentProcessedEvent in Saga for Order Id : {}",
                event.getOrderId());
        try {

//            if(true)
//                throw new Exception();

            ShipOrderCommand shipOrderCommand
                    = ShipOrderCommand
                    .builder()
                    .shipmentId(UUID.randomUUID().toString())
                    .orderId(event.getOrderId())
                    .build();
            commandGateway.send(shipOrderCommand);
        } catch (Exception e) {
            log.error(e.getMessage());
            // Start the compensating transaction
            cancelPaymentCommand(event);
        }
    }

    private void cancelPaymentCommand(PaymentProcessedEvent event) {
        CancelPaymentCommand cancelPaymentCommand
                = new CancelPaymentCommand(
                event.getPaymentId(), event.getOrderId()
        );

        commandGateway.send(cancelPaymentCommand);
    }

    @SagaEventHandler(associationProperty = "orderId")
    public void handle(OrderShippedEvent event) {

        log.info("OrderShippedEvent in Saga for Order Id : {}",
                event.getOrderId());

        CompleteOrderCommand completeOrderCommand
                = CompleteOrderCommand.builder()
                .orderId(event.getOrderId())
                .orderStatus("APPROVED")
                .build();

        commandGateway.send(completeOrderCommand);
    }

    @SagaEventHandler(associationProperty = "orderId")
    @EndSaga
    public void handle(OrderCompletedEvent event) {
        log.info("OrderCompletedEvent in Saga for Order Id : {}",
                event.getOrderId());
    }

    @SagaEventHandler(associationProperty = "orderId")
    @EndSaga
    public void handle(OrderCancelledEvent event) {
        log.info("OrderCancelledEvent in Saga for Order Id : {}",
                event.getOrderId());
    }

    @SagaEventHandler(associationProperty = "orderId")
    public void handle(PaymentCancelledEvent event) {
        log.info("PaymentCancelledEvent in Saga for Order Id : {}",
                event.getOrderId());
        cancelOrderCommand(event.getOrderId());
    }
}
