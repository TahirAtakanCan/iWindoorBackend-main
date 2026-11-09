package com.atablood.iWindoor_api.service;

import com.atablood.iWindoor_api.dto.customer.CustomerDto;
import com.atablood.iWindoor_api.dto.customer.CustomerDto.CustomerProjectLine;
import com.atablood.iWindoor_api.dto.customer.CustomerDto.Request;
import com.atablood.iWindoor_api.entity.Customer;
import com.atablood.iWindoor_api.entity.Project;
import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.exception.ConflictException;
import com.atablood.iWindoor_api.exception.ForbiddenException;
import com.atablood.iWindoor_api.exception.NotFoundException;
import com.atablood.iWindoor_api.repository.CustomerRepository;
import com.atablood.iWindoor_api.repository.ProjectRepository;
import com.atablood.iWindoor_api.security.AccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ProjectRepository projectRepository;
    private final ProjectFinanceService financeService;
    private final AccessGuard guard;

    @Transactional(readOnly = true)
    public List<CustomerDto> list(String q, User user) {
        Long companyId = guard.companyId(user);
        List<Customer> customers = (q == null || q.isBlank())
                ? customerRepository.findByCompanyIdOrderByNameAscIdAsc(companyId)
                : customerRepository.search(companyId, q.trim());
        return customers.stream().map(CustomerDto::of).toList();
    }

    @Transactional(readOnly = true)
    public CustomerDto get(Long id, User user) {
        Customer customer = load(id, user);
        List<CustomerProjectLine> lines = new ArrayList<>();
        BigDecimal totalBalance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (Project p : projectRepository.findAllByCustomer_IdOrderByIdDesc(id)) {
            BigDecimal paid = financeService.paidNet(p.getId());
            BigDecimal target = financeService.target(p, user);
            BigDecimal balance = target.subtract(paid).setScale(2, RoundingMode.HALF_UP);
            lines.add(new CustomerProjectLine(p.getId(), p.getName(),
                    p.getStatus() == null ? null : p.getStatus().name(),
                    target, paid, balance));
            totalBalance = totalBalance.add(balance);
        }
        return CustomerDto.of(customer, lines, totalBalance.setScale(2, RoundingMode.HALF_UP));
    }

    @Transactional
    public CustomerDto create(Request req, User user) {
        Customer c = new Customer();
        c.setCompanyId(guard.companyId(user));
        apply(c, req);
        return CustomerDto.of(customerRepository.save(c));
    }

    @Transactional
    public CustomerDto update(Long id, Request req, User user) {
        Customer c = load(id, user);
        apply(c, req);
        return CustomerDto.of(customerRepository.save(c));
    }

    @Transactional
    public void delete(Long id, User user) {
        load(id, user);
        if (projectRepository.existsByCustomer_Id(id)) {
            throw new ConflictException("CUSTOMER_IN_USE", "Müşteriye bağlı proje varken silinemez.");
        }
        customerRepository.deleteById(id);
    }

    /** Ad+telefon ile eşleştir; yoksa oluştur. Proje snapshot alanlarını da doldurur. */
    @Transactional
    public Customer findOrCreate(Long companyId, String name, String phone, String email, String address) {
        if (name == null || name.isBlank()) return null;
        String n = name.trim();
        String p = blankToNull(phone);
        return customerRepository.findMatch(companyId, n, p).orElseGet(() -> {
            Customer c = new Customer();
            c.setCompanyId(companyId);
            c.setName(n);
            c.setPhone(p);
            c.setEmail(blankToNull(email));
            c.setAddress(blankToNull(address));
            return customerRepository.save(c);
        });
    }

    public Customer requireForCompany(Long customerId, Long companyId) {
        Customer c = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Müşteri", customerId));
        if (!companyId.equals(c.getCompanyId())) {
            throw new ForbiddenException("Bu müşteriye erişim yetkiniz yok.");
        }
        return c;
    }

    private Customer load(Long id, User user) {
        Customer c = customerRepository.findById(id).orElseThrow(() -> new NotFoundException("Müşteri", id));
        if (!guard.companyId(user).equals(c.getCompanyId())) {
            throw new ForbiddenException("Bu müşteriye erişim yetkiniz yok.");
        }
        return c;
    }

    private static void apply(Customer c, Request req) {
        c.setName(req.name().trim());
        c.setPhone(blankToNull(req.phone()));
        c.setEmail(blankToNull(req.email()));
        c.setAddress(blankToNull(req.address()));
        c.setTaxOffice(blankToNull(req.taxOffice()));
        c.setTaxNumber(blankToNull(req.taxNumber()));
        c.setNote(blankToNull(req.note()));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
