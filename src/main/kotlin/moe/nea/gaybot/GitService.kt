package moe.nea.gaybot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File

object GitService {
    val repoRoot = File("repo").absoluteFile
    val logger = LoggerFactory.getLogger("GitService")
    val mutex = Mutex()
    val file = File("emptyfile").absoluteFile.also { it.createNewFile() }
    val sshFile = File("id_rsa")
    fun fetchRepo() {
        logger.info("Fetching remote origin")
        callGit("fetch", "origin")
        logger.info(
            "Repo remote branch master at ${getRemoteCommit("HEAD")}, Local branch master at ${
                getLocalCommit(
                    "master"
                )
            }"
        )
    }

    fun callGit(vararg names: String, passRepoPath: Boolean = true, captureStdout: Boolean = false): Process {
        val pb = ProcessBuilder()
        pb.inheritIO()
        if (captureStdout)
            pb.redirectOutput(ProcessBuilder.Redirect.PIPE)
        pb.environment()["GIT_CONFIG_GLOBAL"] = file.path
        val commands = mutableListOf("git")
        commands.add("-c")
        commands.add("user.name=gaybot")
        commands.add("-c")
        commands.add("user.email=gaybot@notenoughupdates.org")
        commands.add("-c")
        commands.add("core.sshCommand=ssh -i \"${sshFile.absolutePath}\" -o IdentitiesOnly=yes")

        if (passRepoPath) {
            commands.add("-C")
            commands.add(repoRoot.path)
        }
        commands.addAll(names)
        pb.command(commands)
        return pb.start().also { it.waitFor() }
    }

    fun getRemoteCommit(branch: String): String {
        return repoRoot.resolve(".git/refs/remotes/origin/$branch").readText().trim()
    }

    fun parseCommit(committish: String): String {
        return callGit("rev-parse", "--short", committish, captureStdout = true).inputReader().readText().trim()
    }

    fun getLocalCommit(branch: String): String {
        return repoRoot.resolve(".git/refs/heads/$branch").readText().trim()
    }


    fun ensureInitialized() {
        logger.info("Checking for initialized repo at $repoRoot")
        if (repoRoot.exists()) {
            logger.info("Opening existing repo")
        } else {
            logger.info("Downloading new repo")
            callGit(
                "clone",
                "git@github.com:NotEnoughUpdates/NotEnoughUpdates-REPO.git",
                repoRoot.path,
                passRepoPath = false
            )
        }
    }

    fun setRemote(name: String, url: String) {
        logger.info("Setting remote $name to $url")
        callGit("remote", "set-url", name, url)
    }

    fun setHeadTo(committish: String) {
        logger.info("Set current branch ref to $committish")
        callGit("reset", "--hard", committish)
    }

    fun pushHeadTo(remote: String, branch: String) {
        logger.info("Force pushing to $remote $branch")
        callGit("push", "-f", remote, "HEAD:$branch")
    }

    suspend fun <T> useLock(inline: () -> T): T {
 return       mutex.withLock {
            withContext(Dispatchers.IO) {
                inline()
            }
        }
    }


    fun commitFiles(message: String, vararg files: File) {
        logger.info("Committing ${files.joinToString()} with message $message")
        callGit(
            "commit",
            "--allow-empty",
            "-m",
            message.trimStart('-'),
            "--",
            *files.map { it.absolutePath }.toTypedArray()
        )
    }

    fun checkoutBranch(branchname: String) {
        if (callGit("branch", "--", branchname).exitValue() == 0) {
            logger.info("Created branch $branchname")
        }
        logger.info("Switching to branch $branchname")
        callGit("switch", "-f", "--", branchname)
    }

    fun printStatus() {
        callGit("status")
    }
}
