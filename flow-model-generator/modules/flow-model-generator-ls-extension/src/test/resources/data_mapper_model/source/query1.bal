type ContactDetails record {|
    string[] phoneNumbers?;
    string[] addresses?;
|};

type Info record {|
    string[] secondaryPhones;
    string[] emails;
    string[][] addresses;
|};

type User record {|
    Info info;
|};

type Person record {|
    ContactDetails contactDetails;
|};

public function main() {
    User u = {info: {secondaryPhones: [], emails: [], addresses: []}};
    Person p = {
        contactDetails: {
            phoneNumbers: from var secondaryPhonesItem in u.info.secondaryPhones
                select secondaryPhonesItem
        }
    };
}
