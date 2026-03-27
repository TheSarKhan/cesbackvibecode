package com.ces.erp.common.dashboard;

import com.ces.erp.accounting.entity.Invoice;
import com.ces.erp.accounting.repository.InvoiceRepository;
import com.ces.erp.approval.repository.PendingOperationRepository;
import com.ces.erp.contractor.repository.ContractorRepository;
import com.ces.erp.customer.repository.CustomerRepository;
import com.ces.erp.enums.EquipmentStatus;
import com.ces.erp.enums.InvoiceType;
import com.ces.erp.enums.OperationStatus;
import com.ces.erp.enums.ProjectStatus;
import com.ces.erp.enums.RequestStatus;
import com.ces.erp.garage.repository.EquipmentRepository;
import com.ces.erp.investor.repository.InvestorRepository;
import com.ces.erp.operator.repository.OperatorRepository;
import com.ces.erp.project.entity.Project;
import com.ces.erp.project.repository.ProjectRepository;
import com.ces.erp.request.entity.TechRequest;
import com.ces.erp.request.repository.TechRequestRepository;
import com.ces.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CustomerRepository customerRepository;
    private final ContractorRepository contractorRepository;
    private final InvestorRepository investorRepository;
    private final OperatorRepository operatorRepository;
    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final ProjectRepository projectRepository;
    private final TechRequestRepository techRequestRepository;
    private final PendingOperationRepository pendingOperationRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        List<RequestStatus> activeRequestStatuses = List.of(
                RequestStatus.DRAFT, RequestStatus.PENDING,
                RequestStatus.SENT_TO_COORDINATOR, RequestStatus.OFFER_SENT
        );

        List<Project> projects = projectRepository.findAllWithFinances();
        List<TechRequest> requests = techRequestRepository.findAllByDeletedFalse();
        List<Invoice> invoices = invoiceRepository.findAllActive();

        return DashboardStatsResponse.builder()
                .totalCustomers(customerRepository.countByDeletedFalse())
                .totalContractors(contractorRepository.countByDeletedFalse())
                .totalInvestors(investorRepository.countByDeletedFalse())
                .totalOperators(operatorRepository.countByDeletedFalse())
                .totalEmployees(userRepository.countByDeletedFalseAndActiveTrue())
                .availableEquipment(equipmentRepository.countByStatusAndDeletedFalse(EquipmentStatus.AVAILABLE))
                .rentedEquipment(equipmentRepository.countByStatusAndDeletedFalse(EquipmentStatus.RENTED))
                .defectiveEquipment(equipmentRepository.countByStatusAndDeletedFalse(EquipmentStatus.DEFECTIVE))
                .outOfServiceEquipment(equipmentRepository.countByStatusAndDeletedFalse(EquipmentStatus.OUT_OF_SERVICE))
                .pendingApprovals(pendingOperationRepository.countByStatusAndDeletedFalse(OperationStatus.PENDING))
                .activeRequests(techRequestRepository.countByStatusInAndDeletedFalse(activeRequestStatuses))
                .activeProjects(projectRepository.countByStatusAndDeletedFalse(ProjectStatus.ACTIVE))
                .deletedRecords(
                        customerRepository.countByDeletedTrue() +
                        contractorRepository.countByDeletedTrue() +
                        investorRepository.countByDeletedTrue() +
                        operatorRepository.countByDeletedTrue() +
                        equipmentRepository.countByDeletedTrue() +
                        techRequestRepository.countByDeletedTrue() +
                        projectRepository.countByDeletedTrue() +
                        userRepository.countByDeletedTrue()
                )
                .projects(buildProjectDtos(projects))
                .requests(buildRequestDtos(requests))
                .accountingSummary(buildSummary(invoices))
                .invoices(buildInvoiceDtos(invoices))
                .build();
    }

    private List<DashboardStatsResponse.ProjectDto> buildProjectDtos(List<Project> projects) {
        return projects.stream().map(p -> new DashboardStatsResponse.ProjectDto(
                p.getId(),
                p.getStatus() != null ? p.getStatus().name() : null,
                p.getRequest() != null ? p.getRequest().getCompanyName() : null,
                p.getProjectCode(),
                p.getEndDate() != null ? p.getEndDate().toString() : null
        )).toList();
    }

    private List<DashboardStatsResponse.RequestDto> buildRequestDtos(List<TechRequest> requests) {
        return requests.stream().map(r -> new DashboardStatsResponse.RequestDto(
                r.getId(),
                r.getStatus() != null ? r.getStatus().name() : null,
                r.getCompanyName()
        )).toList();
    }

    private DashboardStatsResponse.AccountingSummaryDto buildSummary(List<Invoice> invoices) {
        BigDecimal totalIncome      = sum(invoices, InvoiceType.INCOME);
        BigDecimal totalContractor  = sum(invoices, InvoiceType.CONTRACTOR_EXPENSE);
        BigDecimal totalCompany     = sum(invoices, InvoiceType.COMPANY_EXPENSE);
        return new DashboardStatsResponse.AccountingSummaryDto(
                totalIncome,
                totalContractor,
                totalCompany,
                totalIncome.subtract(totalContractor).subtract(totalCompany),
                count(invoices, InvoiceType.INCOME),
                count(invoices, InvoiceType.CONTRACTOR_EXPENSE),
                count(invoices, InvoiceType.COMPANY_EXPENSE)
        );
    }

    private List<DashboardStatsResponse.InvoiceDto> buildInvoiceDtos(List<Invoice> invoices) {
        return invoices.stream()
                .filter(i -> i.getType() == InvoiceType.INCOME)
                .map(i -> new DashboardStatsResponse.InvoiceDto(
                        i.getInvoiceDate() != null ? i.getInvoiceDate().toString() : null,
                        i.getAmount()
                )).toList();
    }

    private BigDecimal sum(List<Invoice> list, InvoiceType type) {
        return list.stream()
                .filter(i -> i.getType() == type)
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long count(List<Invoice> list, InvoiceType type) {
        return list.stream().filter(i -> i.getType() == type).count();
    }
}
