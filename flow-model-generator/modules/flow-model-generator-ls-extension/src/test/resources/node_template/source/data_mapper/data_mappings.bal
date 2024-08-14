
function transform(Person person, Admission admission) returns Employee => {
    name: person.name,
    empId: admission.empId,
    email: person.email,
    location: {
        city: person.address.city,
        country: person.address.country
    }
};

