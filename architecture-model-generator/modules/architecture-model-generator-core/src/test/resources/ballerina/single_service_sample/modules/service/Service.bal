import single_service_sample.repo;
import single_service_sample.model;

public class AccountService {
    private repo:Repo repository = new();

    public isolated function getAccounts() returns model:Account[] {
        return self.repository.getAllAccounts().toArray();
    }
}