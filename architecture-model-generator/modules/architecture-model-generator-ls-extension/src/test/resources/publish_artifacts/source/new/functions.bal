// Function to check if a string is palindrome
public function isPalindrome(string text) returns boolean {
    string normalized = string:toLowerAscii(text.trim());
    int i = 0;
    int j = normalized.length() - 1;

    while (i < j) {
        if (normalized[i] != normalized[j]) {
            return false;
        }
        i += 1;
        j -= 1;
    }

    return true;
}

// Function to calculate factorial of a number
public function factorial(int n) returns int {
    if (n <= 1) {
        return 1;
    }
    return n * factorial(n - 1);
}

// Function to find the maximum value in an array
public function findMax(int[] numbers) returns int? {
    if (numbers.length() == 0) {
        return ();
    }

    int max = numbers[0];
    foreach int num in numbers {
        if (num > max) {
            max = num;
        }
    }

    return max;
}

// Function to reverse a string
public function reverseString(string text) returns string {
    string reversed = "";
    int i = text.length() - 1;

    while (i >= 0) {
        reversed = reversed + text[i];
        i -= 1;
    }

    return reversed;
}

// Function to check if a number is prime
public function isPrime(int n) returns boolean {
    if (n <= 1) {
        return false;
    }
    if (n <= 3) {
        return true;
    }
    if (n % 2 == 0 || n % 3 == 0) {
        return false;
    }

    int i = 5;
    while (i * i <= n) {
        if (n % i == 0 || n % (i + 2) == 0) {
            return false;
        }
        i += 6;
    }

    return true;
}
