package pl.polsl.take.restaurant.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pl.polsl.take.restaurant.dto.CreateOrderDTO;
import pl.polsl.take.restaurant.dto.CreateOrderItemDTO;
import pl.polsl.take.restaurant.dto.OrderDTO;
import pl.polsl.take.restaurant.model.Customer;
import pl.polsl.take.restaurant.model.Dish;
import pl.polsl.take.restaurant.model.Order;
import pl.polsl.take.restaurant.model.OrderItem;
import pl.polsl.take.restaurant.repository.CustomerRepository;
import pl.polsl.take.restaurant.repository.DishRepository;
import pl.polsl.take.restaurant.repository.OrderRepository;
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final CustomerRepository customerRepo;
    private final DishRepository dishRepo;

    public Order getById(Long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }
    
    @Transactional
    public Order create(CreateOrderDTO dto) {

        Customer customer = customerRepo.findById(dto.getCustomerId())
                .orElseThrow();

        Order order = new Order(customer, dto.getTableNumber());

        dto.getItems().forEach(i -> {
            Dish dish = dishRepo.findById(i.getDishId()).orElseThrow();

            order.getOrderItems().add(
                    new OrderItem(
                            dish,
                            i.getQuantity(),
                            i.getSeatNumber(),
                            i.getNotes(),
                            order
                    )
            );
        });

        return orderRepo.save(order);
    }
    
    public List<Order> getAll() {
        return orderRepo.findAll();
    }
}