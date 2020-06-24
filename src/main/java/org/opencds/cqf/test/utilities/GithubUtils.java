package org.opencds.cqf.test.utilities;

import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.opencds.cqf.testcase.GitHubResourceItems;

import java.io.IOException;
import java.io.InputStream;

public class GithubUtils {

    public static InputStream readContentFromGitHub(GitHubResourceItems artifact, GHRepository repo) {
        if (artifact.getPath() == null) {
            throw new RuntimeException("Path to the resource within the GitHub repository is required");
        }
        
        GHContent content = null;
        try {
            if (artifact.getBranch() != null) {
                GHBranch branch = repo.getBranch(artifact.getBranch());
                String commitHash = branch.getSHA1();
                content = tryRetrievingContent(repo, artifact.getPath(), commitHash);
            }
            else {
                content = tryRetrievingContent(repo, artifact.getPath());
            }
            return content.read();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error reading content from GitHub (" + artifact.getPath() + "): " + e.getMessage());
        }
    }

    public static GHContent tryRetrievingContent(GHRepository repo, String path) throws InterruptedException {
        return tryRetrievingContent(repo, path, "");
    }

    public static GHContent tryRetrievingContent(GHRepository repo, String path, String branch) throws InterruptedException {      
        GHContent content = null;
        for (int i = 0; i < 10; i++) {
          try {
            Thread.sleep(2000);
            content = (branch ==  null || branch.equals("")) ? content = repo.getFileContent(path) : repo.getFileContent(path, branch);
            break;
          } catch (IOException e1) {           
            Thread.sleep(1000);
          }
        }
        return content;
      }
}
