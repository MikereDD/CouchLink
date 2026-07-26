# CouchLink v1.3-dev.7 Test Notes

## Windows build gate
`dotnet build .\src\windows\CouchLink.sln -c Release`

## Live synchronization
1. Open Audio Output and change the default output from the Windows sound flyout; verify the popup updates within about two seconds.
2. Add, remove, enable, or disable an output; verify the list updates without clicking Refresh.
3. Keep Android connected and change the output from Windows; verify Android updates within about three seconds.
4. Switch from Android; verify both Android and the popup converge on the same current output.
5. Rapidly switch several times and confirm there are no freezes, duplicate rows, or stuck loading indicators.
6. Close the popup and disconnect Android; verify no errors or continued UI updates.
7. Confirm manual Refresh still works.
