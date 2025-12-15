# 학습 문서 코드 예시 종합 보고서

**작성일**: 2025-12-13
**목적**: 5개 핵심 학습 문서의 실전 코드 예시 종합 정리
**대상 문서**:
1. `java_practical_design_patterns_2025.md`
2. `clean_architecture_2025.md`
3. `solid_principles_real_cases_2025.md`
4. `code_review_checklist_java_2025.md`
5. `spring_framework_patterns_2025.md`

---

## 1. Java 실전 디자인 패턴

### 1.1 Adapter Pattern - 결제 게이트웨이 통합 (★★★★★)

**문제**: 각 결제사마다 다른 API 인터페이스

```java
// Target Interface (공통 인터페이스)
public interface PaymentGateway {
    PaymentResult processPayment(PaymentRequest request);
}

// Adapter (Stripe를 PaymentGateway로 변환)
@Component
public class StripeAdapter implements PaymentGateway {
    private final StripeAPI stripeAPI;

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        // 공통 요청 → Stripe 형식 변환
        StripeResponse response = stripeAPI.charge(
            request.getAmount(),
            request.getCurrency(),
            request.getToken()
        );
        // Stripe 응답 → 공통 응답 변환
        return new PaymentResult(response.isSuccess(), response.getTransactionId());
    }
}

// PayPal Adapter (동일한 인터페이스)
@Component
public class PayPalAdapter implements PaymentGateway {
    private final PayPalAPI paypalAPI;

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        PayPalRequest ppRequest = new PayPalRequest();
        ppRequest.setAmount(request.getAmount());
        ppRequest.setCurrency(request.getCurrency());
        PayPalResponse response = paypalAPI.processPayment(ppRequest);
        return new PaymentResult(
            response.getStatus() == PayPalStatus.SUCCESS,
            response.getId()
        );
    }
}

// Client - 동일한 인터페이스로 모든 게이트웨이 호출
@Service
public class PaymentService {
    private final Map<String, PaymentGateway> gateways;

    @Autowired
    public PaymentService(List<PaymentGateway> gatewayList) {
        this.gateways = gatewayList.stream()
            .collect(Collectors.toMap(
                g -> g.getClass().getSimpleName(),
                g -> g
            ));
    }

    public PaymentResult processPayment(String gateway, PaymentRequest request) {
        PaymentGateway paymentGateway = gateways.get(gateway);
        return paymentGateway.processPayment(request);
    }
}
```

**장점**:
- 기존 코드 수정 없이 새 시스템 통합
- 게이트웨이 변경 시 Adapter만 교체 (OCP)
- 도메인 모델과 외부 API 분리

---

### 1.2 Decorator Pattern - 알림 서비스 기능 조합 (★★★★☆)

**목적**: 기능 조합의 유연성 (Logging + Caching + Validation)

```java
// Component Interface
public interface NotificationService {
    void send(String message);
}

// Concrete Component (기본 구현)
@Component
public class BasicNotificationService implements NotificationService {
    @Override
    public void send(String message) {
        System.out.println("Sending notification: " + message);
    }
}

// Decorator Base
public abstract class NotificationDecorator implements NotificationService {
    protected NotificationService wrappedService;

    public NotificationDecorator(NotificationService service) {
        this.wrappedService = service;
    }

    @Override
    public void send(String message) {
        wrappedService.send(message);
    }
}

// Concrete Decorator: Logging
public class LoggingDecorator extends NotificationDecorator {
    private final Logger logger = LoggerFactory.getLogger(LoggingDecorator.class);

    public LoggingDecorator(NotificationService service) {
        super(service);
    }

    @Override
    public void send(String message) {
        logger.info("Sending notification: {}", message);
        long start = System.currentTimeMillis();
        wrappedService.send(message);
        long duration = System.currentTimeMillis() - start;
        logger.info("Notification sent in {}ms", duration);
    }
}

// Concrete Decorator: Email Validation
public class EmailValidationDecorator extends NotificationDecorator {
    public EmailValidationDecorator(NotificationService service) {
        super(service);
    }

    @Override
    public void send(String message) {
        if (!isValidEmail(message)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        wrappedService.send(message);
    }

    private boolean isValidEmail(String email) {
        return email.contains("@");
    }
}

// Spring Configuration - 기능 조합
@Configuration
public class NotificationConfig {

    @Bean
    @Primary
    public NotificationService notificationService(BasicNotificationService basic) {
        // 기능 조합: Basic → Email → SMS → Logging
        return new LoggingDecorator(
            new EmailValidationDecorator(
                new SmsFormattingDecorator(basic)
            )
        );
    }

    @Bean
    @Profile("dev")
    public NotificationService devNotificationService(BasicNotificationService basic) {
        // Dev: Logging만
        return new LoggingDecorator(basic);
    }
}
```

**실행 흐름**:
```
Client: notificationService.send("test@example.com")
  ↓
LoggingDecorator: log "Sending notification"
  ↓
EmailValidationDecorator: validate email format
  ↓
SmsFormattingDecorator: format to 160 chars
  ↓
BasicNotificationService: send notification
  ↓
LoggingDecorator: log "Notification sent in Xms"
```

---

### 1.3 Facade Pattern - 주문 처리 단순화 (★★★★☆)

**목적**: 복잡한 서브시스템을 단순한 인터페이스로 감싸기

```java
// 복잡한 서브시스템들
@Service
public class InventoryService {
    public boolean checkStock(Long productId, int quantity) { ... }
    public void reserveStock(Long productId, int quantity) { ... }
}

@Service
public class PaymentService {
    public PaymentResult processPayment(PaymentRequest request) { ... }
}

@Service
public class ShippingService {
    public void scheduleShipping(Order order) { ... }
}

@Service
public class NotificationService {
    public void sendOrderConfirmation(Order order) { ... }
}

// Facade: 복잡한 프로세스를 단순화
@Service
public class OrderFacade {
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final ShippingService shippingService;
    private final NotificationService notificationService;
    private final OrderRepository orderRepository;

    @Autowired
    public OrderFacade(
            InventoryService inventoryService,
            PaymentService paymentService,
            ShippingService shippingService,
            NotificationService notificationService,
            OrderRepository orderRepository) {
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
        this.shippingService = shippingService;
        this.notificationService = notificationService;
        this.orderRepository = orderRepository;
    }

    // 단순한 메서드로 복잡한 프로세스 숨김
    @Transactional
    public Order placeOrder(OrderRequest request) {
        // 1. 재고 확인
        if (!inventoryService.checkStock(request.getProductId(), request.getQuantity())) {
            throw new OutOfStockException("Product out of stock");
        }

        // 2. 주문 생성
        Order order = new Order(request);
        orderRepository.save(order);

        // 3. 재고 예약
        inventoryService.reserveStock(request.getProductId(), request.getQuantity());

        // 4. 결제 처리
        PaymentResult payment = paymentService.processPayment(
            new PaymentRequest(order.getTotalAmount())
        );
        if (!payment.isSuccess()) {
            throw new PaymentFailedException("Payment failed");
        }

        // 5. 배송 스케줄링
        shippingService.scheduleShipping(order);

        // 6. 확인 이메일
        notificationService.sendOrderConfirmation(order);

        return order;
    }
}

// Client (Controller) - 매우 단순!
@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderFacade orderFacade;

    @PostMapping
    public Order createOrder(@RequestBody OrderRequest request) {
        // 복잡한 프로세스가 한 줄로!
        return orderFacade.placeOrder(request);
    }
}
```

---

### 1.4 Chain of Responsibility - 검증 체인 (★★★☆☆)

**목적**: 요청 처리 로직 분리, 처리 순서 유연화

```java
// Handler Interface
public interface ValidationHandler {
    void setNext(ValidationHandler next);
    void validate(OrderRequest request);
}

// Abstract Handler (공통 로직)
public abstract class BaseValidationHandler implements ValidationHandler {
    protected ValidationHandler next;

    @Override
    public void setNext(ValidationHandler next) {
        this.next = next;
    }

    protected void passToNext(OrderRequest request) {
        if (next != null) {
            next.validate(request);
        }
    }
}

// Concrete Handler: Product Validation
public class ProductValidationHandler extends BaseValidationHandler {
    private final ProductRepository productRepository;

    @Override
    public void validate(OrderRequest request) {
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ValidationException("Product not found"));

        if (!product.isActive()) {
            throw new ValidationException("Product is inactive");
        }

        passToNext(request);  // 다음 Handler로
    }
}

// Concrete Handler: Stock Validation
public class StockValidationHandler extends BaseValidationHandler {
    private final InventoryService inventoryService;

    @Override
    public void validate(OrderRequest request) {
        boolean inStock = inventoryService.checkStock(
            request.getProductId(),
            request.getQuantity()
        );

        if (!inStock) {
            throw new ValidationException("Out of stock");
        }

        passToNext(request);
    }
}

// Chain 구성 (Spring Configuration)
@Configuration
public class ValidationChainConfig {

    @Bean
    public ValidationHandler validationChain(
            ProductRepository productRepository,
            InventoryService inventoryService,
            CreditService creditService) {

        // Chain 구성: Product → Stock → Credit
        ValidationHandler productHandler = new ProductValidationHandler(productRepository);
        ValidationHandler stockHandler = new StockValidationHandler(inventoryService);
        ValidationHandler creditHandler = new CreditValidationHandler(creditService);

        productHandler.setNext(stockHandler);
        stockHandler.setNext(creditHandler);

        return productHandler;  // Chain의 시작점
    }
}

// Client (Service)
@Service
public class OrderService {
    private final ValidationHandler validationChain;

    public Order createOrder(OrderRequest request) {
        // Chain 실행 (모든 검증 순차 실행)
        validationChain.validate(request);

        // 검증 통과 → 주문 생성
        return orderRepository.save(new Order(request));
    }
}
```

---

## 2. Clean Architecture 패턴

### 2.1 CQRS - Command/Query 분리

```java
// Command
public record CreateOrderCommand(String CustomerId) implements IRequest<Guid> {}

// Handler
public class CreateOrderHandler implements IRequestHandler<CreateOrderCommand, Guid> {
    private final IApplicationDbContext _db;

    public async Task<Guid> Handle(CreateOrderCommand cmd, CancellationToken ct) {
        var order = new Order(cmd.CustomerId);
        _db.Orders.Add(order);
        await _db.SaveChangesAsync(ct);
        return order.Id;
    }
}
```

**장점**:
- 명확한 책임 분리
- 쉬운 테스트
- 독립적 확장 가능

---

### 2.2 Interface + IoC (Inversion of Control)

**원칙**: Domain/Application에서 계약 정의 → Infrastructure에서 구현

```java
// Domain 또는 Application Layer
public interface IEmailSender {
    Task SendAsync(String to, String subject, String body);
}

// Infrastructure Layer
public class SendGridEmailSender implements IEmailSender {
    private final IConfiguration _config;

    public SendGridEmailSender(IConfiguration config) {
        _config = config;
    }

    public async Task SendAsync(String to, String subject, String body) {
        // SendGrid 구현
    }
}

// DI 등록 (Program.cs)
builder.Services.AddScoped<IEmailSender, SendGridEmailSender>();
```

---

### 2.3 Rich Domain Model vs Anemic Model

```java
// ❌ Anemic Model - 로직 없는 DTO형 엔티티
public class Order {
    public List<OrderItem> Items { get; set; }  // Public setter!
    public decimal Total { get; set; }
}

// ✅ Rich Domain Model - 비즈니스 규칙 캡슐화
public class Order {
    private readonly List<OrderItem> _items = new();
    public IReadOnlyList<OrderItem> Items => _items.AsReadOnly();

    public decimal Total => _items.Sum(i => i.Price * i.Quantity);

    public void AddItem(Product product, int quantity) {
        if (quantity <= 0)
            throw new ArgumentException("Quantity must be positive");

        if (product == null)
            throw new ArgumentNullException(nameof(product));

        _items.Add(new OrderItem(product, quantity));
    }

    public void RemoveItem(Guid itemId) {
        var item = _items.FirstOrDefault(i => i.Id == itemId);
        if (item == null)
            throw new InvalidOperationException("Item not found");

        _items.Remove(item);
    }
}
```

**장점**:
- 비즈니스 규칙이 엔티티 내부에 캡슐화
- 불변성 보장
- 일관성 유지

---

### 2.4 피해야 할 안티패턴

```java
// ❌ UI → Infrastructure 직접 참조
@RestController
public class OrdersController {
    private final AppDbContext _db;  // Infrastructure 직접 참조!

    public async Task<IActionResult> Create(CreateOrderDto dto) {
        var order = new Order(dto.CustomerId);
        _db.Orders.Add(order);
        await _db.SaveChangesAsync();
        return Ok();
    }
}

// ✅ Application 레이어 참조
@RestController
public class OrdersController {
    private final ISender _mediator;  // Application 레이어 참조

    public async Task<IActionResult> Create(CreateOrderCommand cmd) {
        var id = await _mediator.Send(cmd);
        return Ok(new { id });
    }
}
```

---

## 3. SOLID 원칙 실전 사례

### 3.1 SRP (Single Responsibility Principle) - 단일 책임

```java
// ❌ BAD: God Controller (모든 로직이 컨트롤러에)
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequest request) {
        // 검증 로직
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Items cannot be empty");
        }

        // 비즈니스 로직
        double totalPrice = 0;
        for (OrderItem item : request.getItems()) {
            totalPrice += item.getPrice() * item.getQuantity();
        }

        // DB 저장
        String sql = "INSERT INTO orders (customer_id, total_price) VALUES (?, ?)";
        jdbcTemplate.update(sql, request.getCustomerId(), totalPrice);

        // 이메일 발송
        sendOrderConfirmationEmail(request.getCustomerEmail(), totalPrice);

        // 재고 감소
        updateInventory(request.getItems());

        return ResponseEntity.ok(new Order());
    }
}
```

```java
// ✅ GOOD: 책임 분리
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        Order order = orderService.createOrder(request);
        return ResponseEntity.ok(OrderResponse.from(order));
    }
}

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final EmailService emailService;
    private final InventoryService inventoryService;
    private final OrderValidator orderValidator;

    public OrderService(OrderRepository orderRepository,
                       EmailService emailService,
                       InventoryService inventoryService,
                       OrderValidator orderValidator) {
        this.orderRepository = orderRepository;
        this.emailService = emailService;
        this.inventoryService = inventoryService;
        this.orderValidator = orderValidator;
    }

    @Transactional
    public Order createOrder(OrderRequest request) {
        orderValidator.validate(request);
        Order order = Order.create(request);
        Order savedOrder = orderRepository.save(order);
        emailService.sendOrderConfirmation(savedOrder);
        inventoryService.decreaseStock(request.getItems());
        return savedOrder;
    }
}

@Component
public class OrderValidator {
    public void validate(OrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ValidationException("Items cannot be empty");
        }
    }
}
```

**SRP 체크리스트**:
- [ ] 클래스 이름이 "Manager", "Handler", "Util" 같은 모호한 이름인가?
- [ ] 클래스의 public 메서드들이 서로 다른 주제를 다루는가?
- [ ] 단위 테스트 작성 시 많은 Mock이 필요한가?

---

### 3.2 OCP (Open/Closed Principle) - 개방-폐쇄

```java
// ❌ BAD: 새로운 결제 수단 추가 시 기존 코드 수정 필요
public class PaymentProcessor {
    public void processPayment(String paymentType, double amount) {
        if (paymentType.equals("CREDIT_CARD")) {
            processCreditCard(amount);
        } else if (paymentType.equals("PAYPAL")) {
            processPayPal(amount);
        } else if (paymentType.equals("BITCOIN")) {
            // 새로 추가 시 기존 코드 수정!
            processBitcoin(amount);
        }
    }
}
```

```java
// ✅ GOOD: 전략 패턴을 활용한 확장 가능한 설계
public interface PaymentStrategy {
    void processPayment(double amount);
    String getPaymentType();
}

@Component
public class CreditCardPayment implements PaymentStrategy {
    @Override
    public void processPayment(double amount) {
        // 신용카드 결제 로직
    }

    @Override
    public String getPaymentType() {
        return "CREDIT_CARD";
    }
}

@Component
public class PayPalPayment implements PaymentStrategy {
    @Override
    public void processPayment(double amount) {
        // PayPal 결제 로직
    }

    @Override
    public String getPaymentType() {
        return "PAYPAL";
    }
}

// 새로운 결제 수단 추가 시 기존 코드 수정 불필요!
@Component
public class BitcoinPayment implements PaymentStrategy {
    @Override
    public void processPayment(double amount) {
        // Bitcoin 결제 로직
    }

    @Override
    public String getPaymentType() {
        return "BITCOIN";
    }
}

@Service
public class PaymentProcessor {
    private final Map<String, PaymentStrategy> strategies;

    // Spring이 모든 PaymentStrategy 구현체를 자동 주입
    public PaymentProcessor(List<PaymentStrategy> strategyList) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(
                PaymentStrategy::getPaymentType,
                Function.identity()
            ));
    }

    public void processPayment(String paymentType, double amount) {
        PaymentStrategy strategy = strategies.get(paymentType);
        if (strategy == null) {
            throw new UnsupportedPaymentException(paymentType);
        }
        strategy.processPayment(amount);
    }
}
```

---

### 3.3 LSP (Liskov Substitution Principle) - 리스코프 치환

```java
// ❌ BAD: LSP 위반
public interface Bird {
    void fly();
}

public class Penguin implements Bird {
    @Override
    public void fly() {
        // 펭귄은 날 수 없음!
        throw new UnsupportedOperationException("Penguins can't fly");
    }
}

// 사용 시 문제 발생
public void makeBirdFly(Bird bird) {
    bird.fly(); // Penguin 객체 전달 시 예외 발생!
}
```

```java
// ✅ GOOD: LSP 준수
public interface Bird {
    void eat();
    void sleep();
}

public interface FlyingBird extends Bird {
    void fly();
}

public class Sparrow implements FlyingBird {
    @Override
    public void fly() { /* 참새는 날 수 있음 */ }
    @Override
    public void eat() { }
    @Override
    public void sleep() { }
}

public class Penguin implements Bird {
    @Override
    public void eat() { }
    @Override
    public void sleep() { }
    // fly() 메서드 없음 - 정확한 모델링
}

// 안전한 사용
public void makeBirdFly(FlyingBird bird) {
    bird.fly(); // 오직 날 수 있는 새만 받음
}
```

---

### 3.4 ISP (Interface Segregation Principle) - 인터페이스 분리

```java
// ❌ BAD: Fat Interface
public interface Worker {
    void work();
    void eat();
    void sleep();
    void getPaid();
    void attendMeeting();
    void submitReport();
}

// 로봇 직원은 eat(), sleep()이 필요 없음!
public class RobotWorker implements Worker {
    @Override
    public void eat() {
        throw new UnsupportedOperationException("Robots don't eat");
    }
    // ...
}
```

```java
// ✅ GOOD: 인터페이스 분리
public interface Workable {
    void work();
}

public interface Payable {
    void getPaid();
}

public interface Biological {
    void eat();
    void sleep();
}

// 인간 직원
public class HumanWorker implements Workable, Payable, Biological {
    @Override
    public void work() { }
    @Override
    public void getPaid() { }
    @Override
    public void eat() { }
    @Override
    public void sleep() { }
}

// 로봇 직원 - 필요한 인터페이스만 구현
public class RobotWorker implements Workable {
    @Override
    public void work() { }
}
```

---

### 3.5 DIP (Dependency Inversion Principle) - 의존성 역전

```java
// ❌ BAD: 고수준 모듈이 저수준 모듈에 직접 의존
public class OrderService {
    // 구체 클래스에 직접 의존!
    private MySQLOrderRepository orderRepository = new MySQLOrderRepository();
    private EmailNotificationService emailService = new EmailNotificationService();

    public void createOrder(Order order) {
        orderRepository.save(order);
        emailService.sendEmail(order.getCustomerEmail(), "Order created");
    }
}
```

```java
// ✅ GOOD: 추상화에 의존
// 추상화 정의
public interface OrderRepository {
    void save(Order order);
}

public interface NotificationService {
    void sendNotification(String recipient, String message);
}

// 고수준 모듈 - 추상화에 의존
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    // 생성자 주입을 통한 의존성 역전
    public OrderService(OrderRepository orderRepository,
                       NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
    }

    public void createOrder(Order order) {
        orderRepository.save(order);
        notificationService.sendNotification(order.getCustomerEmail(), "Order created");
    }
}

// 저수준 모듈 - 추상화 구현
@Repository
public class MySQLOrderRepository implements OrderRepository {
    @Override
    public void save(Order order) { /* MySQL 저장 로직 */ }
}

// 쉽게 다른 구현체로 교체 가능
@Repository
@Profile("mongodb")
public class MongoOrderRepository implements OrderRepository {
    @Override
    public void save(Order order) { /* MongoDB 저장 로직 */ }
}
```

---

## 4. 코드 리뷰 체크리스트 핵심 예시

### 4.1 기능 & 로직 - 경계 조건 처리

```java
// ❌ 경계 조건 미처리
public int divide(int a, int b) {
    return a / b;  // b가 0일 때?
}

// ✅ 경계 조건 처리
public int divide(int a, int b) {
    if (b == 0) {
        throw new IllegalArgumentException("Divisor cannot be zero");
    }
    return a / b;
}
```

### 4.2 오류 처리

```java
// ❌ 예외를 무시
try {
    userService.deleteUser(id);
} catch (Exception e) {
    // 아무것도 안 함
}

// ✅ 적절한 예외 처리
try {
    userService.deleteUser(id);
} catch (UserNotFoundException e) {
    log.warn("User not found: {}", id);
    throw new ResourceNotFoundException("User not found", e);
} catch (Exception e) {
    log.error("Failed to delete user: {}", id, e);
    throw new ServiceException("User deletion failed", e);
}
```

---

### 4.3 보안 - SQL Injection 방지

```java
// ❌ SQL Injection 취약
public User findByEmail(String email) {
    String sql = "SELECT * FROM users WHERE email = '" + email + "'";
    return jdbcTemplate.queryForObject(sql, userMapper);
}

// ✅ Prepared Statement 사용
public User findByEmail(String email) {
    String sql = "SELECT * FROM users WHERE email = ?";
    return jdbcTemplate.queryForObject(sql, userMapper, email);
}
```

### 4.4 입력 검증

```java
// ❌ 검증 없음
@PostMapping("/users")
public void createUser(@RequestBody UserRequest request) {
    userService.create(request);
}

// ✅ 검증 포함
@PostMapping("/users")
public void createUser(@Valid @RequestBody UserRequest request) {
    userService.create(request);
}

public class UserRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;
}
```

### 4.5 민감 정보 보호

```java
// ❌ 민감 정보 노출
log.info("User login: email={}, password={}", email, password);

// ✅ 민감 정보 마스킹
log.info("User login: email={}", email);
log.debug("Login attempt for user: {}", email);
```

---

### 4.6 성능 - N+1 쿼리 방지

```java
// ❌ N+1 Problem
public List<OrderDTO> getOrders() {
    List<Order> orders = orderRepository.findAll();
    return orders.stream()
        .map(order -> {
            // 각 주문마다 추가 쿼리 발생!
            Customer customer = customerRepository.findById(order.getCustomerId());
            return new OrderDTO(order, customer);
        })
        .collect(Collectors.toList());
}

// ✅ Fetch Join 사용
@Query("SELECT o FROM Order o JOIN FETCH o.customer")
List<Order> findAllWithCustomer();

public List<OrderDTO> getOrders() {
    return orderRepository.findAllWithCustomer().stream()
        .map(OrderDTO::from)
        .collect(Collectors.toList());
}
```

### 4.7 불필요한 객체 생성 방지

```java
// ❌ 반복문 내 객체 생성
for (int i = 0; i < 1000; i++) {
    String message = new String("Hello");
    process(message);
}

// ✅ 재사용
private static final String MESSAGE = "Hello";
for (int i = 0; i < 1000; i++) {
    process(MESSAGE);
}
```

---

### 4.8 Spring Boot - 의존성 주입

```java
// ❌ Field Injection
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
}

// ✅ Constructor Injection
@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}

// ✅ Lombok 사용 시
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
}
```

### 4.9 트랜잭션 관리

```java
// ❌ @Transactional on private method
@Service
public class OrderService {
    @Transactional
    private void saveOrder(Order order) {
        // Spring AOP는 private 메서드에서 동작 안 함!
    }
}

// ✅ @Transactional on public method
@Service
public class OrderService {
    @Transactional
    public void saveOrder(Order order) {
        orderRepository.save(order);
    }
}
```

---

### 4.10 테스트

```java
// ✅ 단위 테스트 예제
@Test
void calculateTotalPrice_withDiscount_shouldApplyCorrectDiscount() {
    // Given
    Order order = new Order();
    order.addItem(new OrderItem("Item1", 100.0));
    order.addItem(new OrderItem("Item2", 200.0));
    order.setDiscountPercentage(10.0);

    // When
    double totalPrice = orderService.calculateTotalPrice(order);

    // Then
    assertThat(totalPrice).isEqualTo(270.0); // 300 - 10% = 270
}

@Test
void calculateTotalPrice_withNullItems_shouldThrowException() {
    // Given
    Order order = new Order();
    order.setItems(null);

    // When & Then
    assertThatThrownBy(() -> orderService.calculateTotalPrice(order))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Items cannot be null");
}
```

---

## 5. Spring Framework 패턴

### 5.1 DI/IoC - Constructor Injection (권장)

```java
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;

    // Spring 4.3+: @Autowired 생략 가능
    public OrderService(OrderRepository orderRepository,
                       PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
    }
}
```

**장점**:
- 불변성 (final)
- 테스트 용이
- 순환 의존성 조기 발견

---

### 5.2 Singleton Pattern - Bean 기본 스코프

```java
@Service  // 기본적으로 Singleton
public class UserService {
    // Spring 컨테이너가 하나의 인스턴스만 생성
}

// Prototype: 요청마다 새 인스턴스
@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ReportGenerator {
}

// Session: HTTP 세션당 하나
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ShoppingCart {
}
```

---

### 5.3 Proxy Pattern - @Transactional

```java
@Service
public class AccountService {

    @Transactional
    public void transfer(Long from, Long to, BigDecimal amount) {
        // Spring이 proxy 생성:
        // 1. 트랜잭션 시작
        // 2. 메서드 실행
        // 3. 커밋 또는 롤백
        debit(from, amount);
        credit(to, amount);
    }
}
```

**내부 동작**:
```
Client → Proxy (TransactionInterceptor)
          ↓
       begin transaction
          ↓
       Target.transfer()
          ↓
       commit/rollback
```

---

### 5.4 AOP (Aspect-Oriented Programming)

```java
@Aspect
@Component
public class LoggingAspect {

    @Before("execution(* com.example.service.*.*(..))")
    public void logBefore(JoinPoint joinPoint) {
        System.out.println("Executing: " + joinPoint.getSignature().getName());
    }

    @AfterReturning(pointcut = "execution(* com.example.service.*.*(..))", returning = "result")
    public void logAfter(JoinPoint joinPoint, Object result) {
        System.out.println("Returned: " + result);
    }
}
```

---

### 5.5 Template Method - JdbcTemplate

```java
@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public User findById(Long id) {
        // Template Method가 처리:
        // 1. Connection 획득
        // 2. PreparedStatement 생성
        // 3. 쿼리 실행
        // 4. ResultSet → Object 변환
        // 5. 리소스 정리 (자동)
        return jdbcTemplate.queryForObject(
            "SELECT * FROM users WHERE id = ?",
            new BeanPropertyRowMapper<>(User.class),
            id
        );
    }

    public int update(User user) {
        return jdbcTemplate.update(
            "UPDATE users SET name = ?, email = ? WHERE id = ?",
            user.getName(),
            user.getEmail(),
            user.getId()
        );
    }
}
```

**장점**:
- 보일러플레이트 제거
- 리소스 관리 자동화
- 예외 변환 (SQLException → DataAccessException)

---

### 5.6 Factory Pattern - @Bean

```java
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setMaximumPoolSize(10);
        return ds;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
```

---

### 5.7 Observer Pattern - Application Events

```java
// 1. Event 정의
public class OrderCreatedEvent extends ApplicationEvent {
    private final Order order;

    public OrderCreatedEvent(Object source, Order order) {
        super(source);
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }
}

// 2. Event Publisher
@Service
public class OrderService {
    private final ApplicationEventPublisher eventPublisher;

    public Order createOrder(Order order) {
        Order saved = orderRepository.save(order);
        eventPublisher.publishEvent(new OrderCreatedEvent(this, saved));
        return saved;
    }
}

// 3. Event Listener
@Component
public class NotificationListener {

    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        Order order = event.getOrder();
        sendEmailNotification(order);
    }

    @EventListener
    @Async  // 비동기 처리
    public void handleOrderCreatedAsync(OrderCreatedEvent event) {
        updateInventory(event.getOrder());
    }
}

// 4. @TransactionalEventListener
@Component
public class TransactionalListener {

    // 트랜잭션 커밋 후에만 실행
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAfterCommit(OrderCreatedEvent event) {
        sendOrderConfirmation(event.getOrder());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleAfterRollback(OrderCreatedEvent event) {
        logFailure(event.getOrder());
    }
}
```

---

### 5.8 Strategy Pattern - 결제 전략

```java
public interface PaymentStrategy {
    void pay(BigDecimal amount);
}

@Component
public class CreditCardPayment implements PaymentStrategy {
    @Override
    public void pay(BigDecimal amount) {
        // 신용카드 결제
    }
}

@Component
public class PayPalPayment implements PaymentStrategy {
    @Override
    public void pay(BigDecimal amount) {
        // PayPal 결제
    }
}

@Service
public class PaymentService {
    private final Map<String, PaymentStrategy> strategies;

    @Autowired
    public PaymentService(List<PaymentStrategy> strategyList) {
        // Spring이 모든 구현체 주입
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(
                s -> s.getClass().getSimpleName(),
                s -> s
            ));
    }

    public void processPayment(String type, BigDecimal amount) {
        PaymentStrategy strategy = strategies.get(type);
        strategy.pay(amount);
    }
}
```

---

### 5.9 MVC Pattern

```
Request
  ↓
DispatcherServlet (Front Controller)
  ↓
HandlerMapping (Controller 선택)
  ↓
Controller (비즈니스 로직)
  ↓
Model (데이터)
  ↓
ViewResolver (View 선택)
  ↓
View (렌더링)
  ↓
Response
```

```java
// Controller
@Controller
@RequestMapping("/products")
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public String listProducts(Model model) {
        List<Product> products = productService.findAll();
        model.addAttribute("products", products);
        return "product-list";
    }
}

// Service (비즈니스 로직)
@Service
public class ProductService {
    private final ProductRepository repository;

    public List<Product> findAll() {
        return repository.findAll();
    }
}

// Repository (데이터 액세스)
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategory(String category);
}
```

---

## 핵심 정리 표

| 패턴 | 중요도 | 사용 시기 | 실전 사례 |
|-----|--------|----------|----------|
| **Adapter** | ★★★★★ | 외부 API 통합 | 결제, SMS, 이메일 게이트웨이 |
| **Decorator** | ★★★★☆ | 기능 동적 추가 | 로깅, 캐싱, 검증 |
| **Facade** | ★★★★☆ | 복잡한 시스템 단순화 | 주문 처리, API Gateway |
| **Chain** | ★★★☆☆ | 다단계 검증/처리 | Validation, Filter |
| **Strategy** | ★★★★☆ | 알고리즘 교체 | 결제, 알림, 할인 |
| **Observer** | ★★★★☆ | 이벤트 기반 처리 | 비동기 알림, 로깅 |
| **Template** | ★★★★☆ | 보일러플레이트 제거 | JdbcTemplate, RestTemplate |
| **Proxy** | ★★★★★ | 횡단 관심사 | @Transactional, AOP |

---

## 프로덕션 체크리스트

### 설계
- [ ] Constructor injection 사용
- [ ] Interface로 추상화
- [ ] Singleton bean thread-safe 확인
- [ ] SOLID 원칙 준수

### 보안
- [ ] SQL Injection 방지
- [ ] 입력 검증 (@Valid)
- [ ] 민감 정보 로깅 금지

### 성능
- [ ] N+1 쿼리 없음
- [ ] 적절한 캐싱 (@Cacheable)
- [ ] 페이징 처리

### 테스트
- [ ] 단위 테스트 작성
- [ ] Given-When-Then 패턴
- [ ] 엣지 케이스 커버

---

## 참고 자료

- DigitalOcean - Design Patterns Tutorial (2024)
- Refactoring.Guru - Design Patterns in Java
- Google Engineering Practices - Code Review
- Spring Framework Documentation v6.1
- Baeldung - Spring Boot Best Practices (2025)

---

**마지막 업데이트**: 2025-12-13
**버전**: 1.0
