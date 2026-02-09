


# 1. Stage all changes
git add .





# 2. Save changes to a temporary stash
git stash

# 3. Ensure merge strategy is set (only needs to be run once, but safe here)
# git config pull.rebase false

# 4. Pull the latest changes from GitHub
git pull origin v4

# 5. Bring your changes back from the stash
git stash pop

# 6. Now commit the merged work
git commit -m "test" -a


# 7. Push to GitHub
git push origin v4