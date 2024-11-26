
function transformAdmission(Person person, Admission admission) returns Employee => {
    name: person.name,
    empId: admission.empId,
    email: person.email,
    location: {
        city: person.address.city,
        country: person.address.country
    }
};

function transformToPerson(string name, Address address) returns Person => {
    name: name,
    email: "email",
    address: address
};

