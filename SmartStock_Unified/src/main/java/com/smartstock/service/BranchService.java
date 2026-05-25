package com.smartstock.service;

import com.smartstock.dao.BranchDAO;
import com.smartstock.model.Branch;
import java.util.List;

public class BranchService {
    private final BranchDAO branchDAO;

    public BranchService() {
        this.branchDAO = new BranchDAO();
    }

    public Branch createBranch(String name, String location, String phone, String email) {
        Branch branch = new Branch(name, location);
        branch.setPhone(phone);
        branch.setEmail(email);
        int id = branchDAO.insert(branch);
        if (id > 0) {
            branch.setId(id);
            return branch;
        }
        return null;
    }

    public boolean updateBranch(Branch branch) {
        return branchDAO.update(branch);
    }

    public boolean deleteBranch(int id) {
        return branchDAO.delete(id);
    }

    public Branch getBranchById(int id) {
        return branchDAO.findById(id);
    }

    public List<Branch> getAllBranches() {
        return branchDAO.findAll();
    }
}
