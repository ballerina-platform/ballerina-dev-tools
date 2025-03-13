import ballerinax/np;

function suggestMovieName(string genre, int n, np:Prompt prompt = `
    **Assumption**
    Think yourself as a movie expert

    **What to do**
    Suggest a movie name matching to the genre given:
    ${genre}

    **Output**
    string - The suggested movie name
`) returns string|error = @np:NaturalFunction external;

function rateMovie(string movieName, np:Context context, np:Prompt prompt = `**Assumption**
Think yourself as a movie expert

**What to do**
Give rating for the movie ${movieName} out of 10 based on your opinion

**Output**
int - number between 1 and 10 as the rating
`) returns int|error = @np:NaturalFunction external;

function summarizeBlog(Blog blog, np:Prompt prompt = `
    Think yourself as a blog reviewer and summerize the following blog

    **title**
    ${blog.title}

    **content**
    ${blog.content}

    Output must be in {title: string, summary: string} type
`) returns Summary|error = @np:NaturalFunction external;

function cleanCode() returns string {
    return "cleaned";
}
