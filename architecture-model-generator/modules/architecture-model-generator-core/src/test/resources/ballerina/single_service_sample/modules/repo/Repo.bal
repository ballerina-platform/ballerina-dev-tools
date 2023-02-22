import single_service_sample.model;

public class Repo {

    private table<model:Account> key(AccountId) accounts = table[];

    public isolated function getAllAccounts() returns table<model:Account> key(AccountId) {
        return self.accounts;
    }
}
