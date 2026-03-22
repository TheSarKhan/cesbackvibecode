package com.ces.erp.common.dashboard;

import com.ces.erp.approval.repository.PendingOperationRepository;
import com.ces.erp.contractor.repository.ContractorRepository;
import com.ces.erp.customer.repository.CustomerRepository;
import com.ces.erp.enums.EquipmentStatus;
import com.ces.erp.enums.OperationStatus;
import com.ces.erp.enums.ProjectStatus;
import com.ces.erp.enums.RequestStatus;
import com.ces.erp.garage.repository.EquipmentRepository;
import com.ces.erp.investor.repository.InvestorRepository;
import com.ces.erp.operator.repository.OperatorRepository;
import com.ces.erp.project.repository.ProjectRepository;
import com.ces.erp.request.repository.TechRequestRepository;
import com.ces.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        List<RequestStatus> activeRequestStatuses = List.of(
                RequestStatus.DRAFT, RequestStatus.PENDING,
                RequestStatus.SENT_TO_COORDINATOR, RequestStatus.OFFER_SENT
        );

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
                .build();
    }
}
