package org.o7planning.springmvconlinestore.dao.impl;
 
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
 
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.o7planning.springmvconlinestore.dao.CustomerDAO;
import org.o7planning.springmvconlinestore.dao.OrderDAO;
import org.o7planning.springmvconlinestore.dao.ProductDAO;
import org.o7planning.springmvconlinestore.entity.Customer;
import org.o7planning.springmvconlinestore.entity.Order;
import org.o7planning.springmvconlinestore.entity.OrderDetail;
import org.o7planning.springmvconlinestore.entity.Product;
import org.o7planning.springmvconlinestore.model.CartInfo;
import org.o7planning.springmvconlinestore.model.CartLineInfo;
import org.o7planning.springmvconlinestore.model.CustomerInfo;
import org.o7planning.springmvconlinestore.model.OrderDetailInfo;
import org.o7planning.springmvconlinestore.model.OrderInfo;
import org.o7planning.springmvconlinestore.model.PaginationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
 
//Transactional for Hibernate
@Transactional
public class OrderDAOImpl implements OrderDAO {
 
    @Autowired
    private SessionFactory sessionFactory;
 
    @Autowired
    private ProductDAO productDAO;
    
    @Autowired
    private CustomerDAO customerDAO;
 
    private int getMaxOrderNum() {
        String sql = "Select max(o.orderNum) from " + Order.class.getName() + " o ";
        Session session = sessionFactory.getCurrentSession();
        Query query = session.createQuery(sql);
        Integer value = (Integer) query.uniqueResult();
        if (value == null) {
            return 0;
        }
        return value;
    }
 
    public void saveOrder(CartInfo cartInfo) {
        Session session = sessionFactory.getCurrentSession();
        
        Calendar today = Calendar.getInstance();
		today.set(Calendar.HOUR_OF_DAY, 0);
		
 
        int orderNum = this.getMaxOrderNum() + 1;
        Order order = new Order();
 
        order.setAmount(cartInfo.getQuantityTotal());
        order.setProdRetailPrice(cartInfo.getAmountTotal()); //Skewed logic - Have Total Field instead?
 
        CustomerInfo customerInfo = cartInfo.getCustomerInfo();
        
        int customerId = customerDAO.findCustomerId(customerInfo.getEmail(), customerInfo.getPassword());
        order.setCustomerID(customerId);
        /*
        order.setCustomerName(customerInfo.getFirstName());
        order.setCustomerName(customerInfo.getLastName());
        order.setCustomerEmail(customerInfo.getEmail());
        order.setCustomerPhone(customerInfo.getPhone());
        order.setCustomerAddress(customerInfo.getAddress());
 		*/
        session.persist(order);
 
        List<CartLineInfo> lines = cartInfo.getCartLines();
 
        for (CartLineInfo line : lines) {
            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setAmount(line.getAmount());
            detail.setPrice(line.getProductInfo().getPrice());
            detail.setQuanity(line.getQuantity());
 
            int code = line.getProductInfo().getCode();
            Product product = this.productDAO.findProduct(code);
            detail.setProduct(product);
 
            session.persist(detail);
        }
 
        // Set OrderNum for report.
        cartInfo.setOrderNum(orderNum);
    }
 
    // @page = 1, 2, ...
    public PaginationResult<OrderInfo> listOrderInfo(int page, int maxResult, int maxNavigationPage) {
        String sql = "Select new " + OrderInfo.class.getName()//
                + "(ord.id, ord.orderDate, ord.orderNum, ord.amount, "
                + " ord.customerName, ord.customerAddress, ord.customerEmail, ord.customerPhone) " + " from "
                + Order.class.getName() + " ord "//
                + " order by ord.orderNum desc";
        Session session = this.sessionFactory.getCurrentSession();
 
        Query query = session.createQuery(sql);
 
        return new PaginationResult<OrderInfo>(query, page, maxResult, maxNavigationPage);
    }
 
    public Order findSingleOrder(int orderId) {
        Session session = sessionFactory.getCurrentSession();
        Criteria crit = session.createCriteria(Order.class);
        crit.add(Restrictions.eq("Order_ID", orderId));
        return (Order) crit.uniqueResult();
    }
    
    public Order findOrderForCustomer(int orderId, int customerId) {
    	Session session = sessionFactory.getCurrentSession();
        Criteria crit = session.createCriteria(Order.class);
        crit.add(Restrictions.eq("Order_ID", orderId));
        crit.add(Restrictions.eq("Customer_ID", customerId));
        return (Order) crit.uniqueResult();
    }
 
    public OrderInfo getOrderInfo(int orderId) {
        Order order = this.findSingleOrder(orderId);
        if (order == null) {
            return null;
        }
        Customer customer = customerDAO.lookUpCustomerWithID(order.getCustomerID());
        return new OrderInfo(order.getId(), order.getOrderDate(), //
                order.getId(), order.getAmount(), customer.getFirstName(), //
                customer.getLastName(), customer.getAddress(), customer.getCity(), customer.getState(), //
                customer.getZip(), customer.getEmail(), customer.getPhone());
    }
 
    public List<OrderDetailInfo> listOrderDetailInfos(int orderId) {
        String sql = "Select new " + OrderDetailInfo.class.getName() //
                + "(d.id, d.product.code, d.product.name , d.quanity,d.price,d.amount) "//
                + " from " + OrderDetail.class.getName() + " d "//
                + " where d.order.id = :orderId ";
 
        Session session = this.sessionFactory.getCurrentSession();
 
        Query query = session.createQuery(sql);
        query.setParameter("orderId", orderId);
 
        return query.list();
    }

	
 
}