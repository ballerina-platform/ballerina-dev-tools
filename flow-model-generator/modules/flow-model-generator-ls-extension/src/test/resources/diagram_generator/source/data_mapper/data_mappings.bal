function transformToPerson(string name, string email, Address address) returns Person => {
    name: name,
    email: email,
    address: address
};

function transformToEmployee(Person person, Admission admission) returns Employee => {
    name: person.name,
    empId: admission.empId,
    email: person.email,
    location: {
        city: person.address.city,
        country: person.address.country
    }
};
