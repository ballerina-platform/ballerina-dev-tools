import ballerinax/np;

function suggestMovieGenre(string input, np:Prompt prompt = `
    **Assumption**
    Think yourself as a movie expert

    **What to do**
    Suggest a movie genre matching to the input given:
    ${input}

    **Output**
    string - The suggested movie genre
`) returns string|error = @np:NaturalFunction external;

function suggestMovieName1(string genre, int n, np:Prompt prompt = `
    **Assumption**
    Think yourself as a movie expert

    **What to do**
    Suggest a movie name matching to the genre given:
    ${genre}

    **Output**
    string - The suggested movie name
`) returns string|error = @np:NaturalFunction external;

function suggestMovieName2(np:Prompt prompt = `
    **Assumption**
    Think yourself as a movie expert

    **What to do**
    Suggest a movie name

    **Output**
    string - The suggested movie name
`) returns string|error = @np:NaturalFunction external;

function rateMovie(string movieName, np:Context context, np:Prompt prompt = `
    **Assumption**
    Think yourself as a movie expert

    **What to do**
    Give rating for the movie ${movieName} out of 10 based on your opinion

    **Output**
    int - number between 1 and 10 as the rating
`) returns int|error = @np:NaturalFunction external;
