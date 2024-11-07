public function transform(Source src) returns Target {
    return {
        fullName: string `${src.firstName} ${src.lastName}`,
        age: src.age
    };
}
