package com.ecommerce.api.customer;

import com.ecommerce.api.customer.dto.CustomerRequest;
import com.ecommerce.api.customer.dto.CustomerResponse;
import com.ecommerce.api.exception.BusinessException;
import com.ecommerce.api.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L)
                .name("Joao")
                .email("joao@teste.com")
                .phone("11999999999")
                .address("Rua A, 123")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void deveCriarClienteQuandoEmailNaoExiste() {
        CustomerRequest request = new CustomerRequest("Joao", "joao@teste.com", "11999999999", "Rua A, 123");
        when(customerRepository.existsByEmail("joao@teste.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        CustomerResponse response = customerService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("joao@teste.com");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void naoDeveCriarClienteComEmailJaCadastrado() {
        CustomerRequest request = new CustomerRequest("Joao", "joao@teste.com", "11999999999", "Rua A, 123");
        when(customerRepository.existsByEmail("joao@teste.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("email");

        verify(customerRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoAoBuscarClienteInexistente() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deveListarTodosOsClientes() {
        when(customerRepository.findAll()).thenReturn(List.of(customer));

        List<CustomerResponse> result = customerService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Joao");
    }

    @Test
    void deveAtualizarDadosDoCliente() {
        CustomerRequest request = new CustomerRequest("Joao Atualizado", "joao.novo@teste.com", "11888888888", "Rua B, 456");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerResponse response = customerService.update(1L, request);

        assertThat(response.name()).isEqualTo("Joao Atualizado");
        assertThat(response.email()).isEqualTo("joao.novo@teste.com");
    }

    @Test
    void deveRemoverClienteExistente() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        customerService.delete(1L);

        verify(customerRepository).delete(customer);
    }
}
