package com.ces.erp.common.search;

import com.ces.erp.contractor.repository.ContractorRepository;
import com.ces.erp.customer.repository.CustomerRepository;
import com.ces.erp.garage.repository.EquipmentRepository;
import com.ces.erp.investor.repository.InvestorRepository;
import com.ces.erp.operator.repository.OperatorRepository;
import com.ces.erp.project.repository.ProjectRepository;
import com.ces.erp.request.repository.TechRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final CustomerRepository customerRepository;
    private final ContractorRepository contractorRepository;
    private final EquipmentRepository equipmentRepository;
    private final TechRequestRepository techRequestRepository;
    private final ProjectRepository projectRepository;
    private final InvestorRepository investorRepository;
    private final OperatorRepository operatorRepository;

    @Transactional(readOnly = true)
    public List<SearchResultItem> search(String q) {
        if (q == null || q.trim().length() < 2) return List.of();
        String lower = q.toLowerCase().trim();
        List<SearchResultItem> results = new ArrayList<>();

        // Search customers by companyName, supplierPerson, officeContactPerson, voen
        customerRepository.findAllByDeletedFalse().stream()
                .filter(c -> matches(lower, c.getCompanyName(), c.getSupplierPerson(), c.getOfficeContactPerson(), c.getVoen()))
                .limit(5)
                .forEach(c -> results.add(SearchResultItem.builder()
                        .id(c.getId()).type("MÜŞTƏRİ").label(c.getCompanyName())
                        .subLabel(c.getVoen()).path("/customers").build()));

        // Search contractors by companyName, contactPerson, voen
        contractorRepository.findAllByDeletedFalse().stream()
                .filter(c -> matches(lower, c.getCompanyName(), c.getContactPerson(), c.getVoen()))
                .limit(5)
                .forEach(c -> results.add(SearchResultItem.builder()
                        .id(c.getId()).type("PODRATÇI").label(c.getCompanyName())
                        .subLabel(c.getVoen()).path("/contractors").build()));

        // Search investors by companyName, contactPerson, voen
        investorRepository.findAllByDeletedFalse().stream()
                .filter(c -> matches(lower, c.getCompanyName(), c.getContactPerson(), c.getVoen()))
                .limit(5)
                .forEach(c -> results.add(SearchResultItem.builder()
                        .id(c.getId()).type("İNVESTOR").label(c.getCompanyName())
                        .subLabel(c.getVoen()).path("/investors").build()));

        // Search operators by firstName, lastName
        operatorRepository.findAllActive().stream()
                .filter(o -> matches(lower, o.getFirstName(), o.getLastName()))
                .limit(5)
                .forEach(o -> results.add(SearchResultItem.builder()
                        .id(o.getId()).type("OPERATOR").label(o.getFirstName() + " " + o.getLastName())
                        .path("/operators").build()));

        // Search equipment by equipmentCode, name, brand, model, serialNumber
        equipmentRepository.findAllByDeletedFalse().stream()
                .filter(e -> matches(lower, e.getEquipmentCode(), e.getName(), e.getBrand(), e.getModel(), e.getSerialNumber()))
                .limit(5)
                .forEach(e -> results.add(SearchResultItem.builder()
                        .id(e.getId()).type("TEXNİKA")
                        .label((e.getBrand() != null ? e.getBrand() + " " : "") + (e.getModel() != null ? e.getModel() : e.getName()))
                        .subLabel(e.getEquipmentCode()).path("/garage").build()));

        // Search requests by requestCode, companyName, contactPerson, notes
        techRequestRepository.findAllByDeletedFalse().stream()
                .filter(r -> matches(lower, r.getRequestCode(), r.getCompanyName(), r.getContactPerson(), r.getNotes()))
                .limit(5)
                .forEach(r -> results.add(SearchResultItem.builder()
                        .id(r.getId()).type("SORĞU").label(r.getRequestCode() != null ? r.getRequestCode() : r.getCompanyName())
                        .subLabel(r.getCompanyName())
                        .path("/requests").build()));

        // Search projects by projectCode, and linked request's companyName
        projectRepository.findAllWithFinances().stream()
                .filter(p -> matches(lower, p.getProjectCode(),
                        p.getRequest() != null ? p.getRequest().getCompanyName() : null))
                .limit(5)
                .forEach(p -> results.add(SearchResultItem.builder()
                        .id(p.getId()).type("LAYİHƏ").label(p.getProjectCode())
                        .subLabel(p.getRequest() != null ? p.getRequest().getCompanyName() : null)
                        .path("/projects").build()));

        return results;
    }

    private boolean matches(String q, String... fields) {
        for (String f : fields) {
            if (f != null && f.toLowerCase().contains(q)) return true;
        }
        return false;
    }
}
